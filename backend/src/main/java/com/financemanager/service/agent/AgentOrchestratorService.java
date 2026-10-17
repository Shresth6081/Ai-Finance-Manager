package com.financemanager.service.agent;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.financemanager.dto.AgentActionProposal;
import com.financemanager.dto.AgentChatRequest;
import com.financemanager.dto.AgentStreamEvent;
import com.financemanager.model.Category;
import com.financemanager.model.User;
import com.financemanager.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AgentOrchestratorService {

    private final ChatModel chatModel;
    private final FinancialToolService toolService;
    private final CategoryRepository categoryRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final int MAX_ITERATIONS = 4;
    private static final Pattern THOUGHT_PATTERN = Pattern.compile("(?i)THOUGHT:\\s*(.*?)(?=(?:ACTION:|PROPOSAL:|FINAL_ANSWER:|$))", Pattern.DOTALL);
    private static final Pattern ACTION_PATTERN = Pattern.compile("(?i)ACTION:\\s*([a-zA-Z0-9_]+)", Pattern.DOTALL);
    private static final Pattern FINAL_ANSWER_PATTERN = Pattern.compile("(?i)FINAL_ANSWER:\\s*(.*)", Pattern.DOTALL);

    public String executeReActLoop(User user, String prompt, List<AgentChatRequest.ChatMessage> history, Consumer<AgentStreamEvent> emitter) {
        Long userId = user.getId();
        String username = user.getUsername();
        LocalDate today = LocalDate.now();

        emitter.accept(AgentStreamEvent.thought("Analyzing user request and identifying required financial tools..."));

        List<Category> userCategories = categoryRepository.findByUser(user);
        String categorySummary = userCategories.stream()
                .map(Category::getName)
                .distinct()
                .collect(Collectors.joining(", "));

        StringBuilder contextBuilder = new StringBuilder();
        contextBuilder.append("System Date: ").append(today.toString()).append("\n");
        contextBuilder.append("User: ").append(username).append(" (User ID: ").append(userId).append(")\n");
        contextBuilder.append("User's Existing Categories: [").append(categorySummary.isEmpty() ? "None" : categorySummary).append("]\n\n");

        if (history != null && !history.isEmpty()) {
            contextBuilder.append("Conversation History:\n");
            for (AgentChatRequest.ChatMessage msg : history) {
                contextBuilder.append(msg.getSender().toUpperCase()).append(": ").append(msg.getText()).append("\n");
            }
            contextBuilder.append("\n");
        }

        contextBuilder.append("User Request: \"").append(prompt).append("\"\n\n");

        String systemPrompt = buildSystemPrompt(userId);

        StringBuilder executionLog = new StringBuilder();
        executionLog.append(contextBuilder);

        for (int iteration = 1; iteration <= MAX_ITERATIONS; iteration++) {
            String fullPrompt = systemPrompt + "\n\nExecution Transcript so far:\n" + executionLog.toString() + "\nRespond with your next THOUGHT and either ACTION, PROPOSAL, or FINAL_ANSWER:";

            log.info("Agent iteration {} for user {}", iteration, username);
            String modelOutput = callLlm(fullPrompt, 45);

            if (modelOutput == null || modelOutput.isBlank()) {
                String fallback = "I encountered a momentary issue processing your request. Please try again.";
                emitter.accept(AgentStreamEvent.done(fallback));
                return fallback;
            }

            // Extract Thought
            Matcher thoughtMatcher = THOUGHT_PATTERN.matcher(modelOutput);
            if (thoughtMatcher.find()) {
                String thought = thoughtMatcher.group(1).trim();
                if (!thought.isEmpty()) {
                    emitter.accept(AgentStreamEvent.thought(thought));
                }
            }

            // Extract Proposal with balanced brace parser
            String proposalJson = extractBalancedJson(modelOutput, "PROPOSAL:");
            if (proposalJson != null && !proposalJson.contains("...") && proposalJson.contains("\"")) {
                try {
                    Map<String, Object> map = objectMapper.readValue(proposalJson, new TypeReference<>() {});
                    AgentActionProposal proposal = AgentActionProposal.builder()
                            .actionId(UUID.randomUUID().toString())
                            .actionType((String) map.getOrDefault("actionType", "CUSTOM_ACTION"))
                            .title((String) map.getOrDefault("title", "Proposed Action"))
                            .description((String) map.getOrDefault("description", "Agent proposed an action."))
                            .parameters((Map<String, Object>) map.getOrDefault("parameters", Collections.emptyMap()))
                            .status("PENDING")
                            .build();

                    emitter.accept(AgentStreamEvent.actionRequired(proposal));
                } catch (Exception e) {
                    log.warn("Failed to parse proposal JSON: {}", proposalJson, e);
                }
            }

            // Extract Final Answer
            Matcher finalMatcher = FINAL_ANSWER_PATTERN.matcher(modelOutput);
            if (finalMatcher.find()) {
                String finalAns = finalMatcher.group(1).trim();
                streamTokens(finalAns, emitter);
                emitter.accept(AgentStreamEvent.done(finalAns));
                return finalAns;
            }

            // Extract Action Tool Call
            Matcher actionMatcher = ACTION_PATTERN.matcher(modelOutput);
            if (actionMatcher.find()) {
                String toolName = actionMatcher.group(1).trim();
                String inputJson = extractBalancedJson(modelOutput, "ACTION_INPUT:");
                if (inputJson == null || inputJson.contains("...") || !inputJson.contains("{")) {
                    inputJson = "{}";
                }

                emitter.accept(AgentStreamEvent.toolCall(toolName, inputJson, "Running tool: " + toolName));

                // Execute Tool
                String observation = executeTool(userId, toolName, inputJson);
                emitter.accept(AgentStreamEvent.toolResult(toolName, observation));

                executionLog.append("\nTHOUGHT: ").append(thoughtMatcher.find() ? thoughtMatcher.group(1).trim() : "Proceeding with tool.")
                        .append("\nACTION: ").append(toolName)
                        .append("\nACTION_INPUT: ").append(inputJson)
                        .append("\nOBSERVATION: ").append(observation).append("\n");

                continue; // continue ReAct loop with new observation
            }

            // If no action or final_answer tag matched, treat the whole output as final answer
            String cleanOutput = cleanModelOutput(modelOutput);
            streamTokens(cleanOutput, emitter);
            emitter.accept(AgentStreamEvent.done(cleanOutput));
            return cleanOutput;
        }

        // If loop completes without explicit FINAL_ANSWER, summarize findings
        String finalSummaryPrompt = systemPrompt + "\n\nTranscript:\n" + executionLog.toString() + "\nProvide your FINAL_ANSWER directly to the user based on the observations gathered above:";
        String finalAns = cleanModelOutput(callLlm(finalSummaryPrompt, 45));
        streamTokens(finalAns, emitter);
        emitter.accept(AgentStreamEvent.done(finalAns));
        return finalAns;
    }

    private String extractBalancedJson(String text, String prefix) {
        if (text == null) return null;
        int idx = text.toUpperCase().indexOf(prefix.toUpperCase());
        if (idx == -1) return null;

        int startPos = -1;
        char openChar = '{';
        char closeChar = '}';

        for (int i = idx + prefix.length(); i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '{') {
                startPos = i;
                openChar = '{';
                closeChar = '}';
                break;
            } else if (c == '[') {
                startPos = i;
                openChar = '[';
                closeChar = ']';
                break;
            }
        }

        if (startPos == -1) return null;

        int depth = 0;
        boolean inQuotes = false;
        boolean isEscaped = false;

        for (int i = startPos; i < text.length(); i++) {
            char c = text.charAt(i);

            if (c == '\\' && !isEscaped) {
                isEscaped = true;
                continue;
            }

            if (c == '"' && !isEscaped) {
                inQuotes = !inQuotes;
            }

            if (!inQuotes) {
                if (c == openChar) {
                    depth++;
                } else if (c == closeChar) {
                    depth--;
                    if (depth == 0) {
                        return text.substring(startPos, i + 1).trim();
                    }
                }
            }

            isEscaped = false;
        }

        return null;
    }

    private String executeTool(Long userId, String toolName, String inputJson) {
        try {
            if (inputJson == null || inputJson.isBlank() || inputJson.contains("...") || !inputJson.contains("{")) {
                inputJson = "{}";
            }
            Map<String, Object> params = objectMapper.readValue(inputJson, new TypeReference<>() {});

            switch (toolName) {
                case "getMonthlySpendingByCategory": {
                    Integer m = params.get("month") != null ? ((Number) params.get("month")).intValue() : null;
                    Integer y = params.get("year") != null ? ((Number) params.get("year")).intValue() : null;
                    Map<String, Object> res = toolService.getMonthlySpendingByCategory(userId, m, y);
                    return objectMapper.writeValueAsString(res);
                }
                case "getHistoricalTrends": {
                    Integer count = params.get("monthsCount") != null ? ((Number) params.get("monthsCount")).intValue() : 6;
                    Map<String, Object> res = toolService.getHistoricalTrends(userId, count);
                    return objectMapper.writeValueAsString(res);
                }
                case "getRecurringTransactions": {
                    Map<String, Object> res = toolService.getRecurringTransactions(userId);
                    return objectMapper.writeValueAsString(res);
                }
                case "getBudgetVsActualReport": {
                    Integer m = params.get("month") != null ? ((Number) params.get("month")).intValue() : null;
                    Integer y = params.get("year") != null ? ((Number) params.get("year")).intValue() : null;
                    Map<String, Object> res = toolService.getBudgetVsActualReport(userId, m, y);
                    return objectMapper.writeValueAsString(res);
                }
                case "simulateSavingsTrajectory": {
                    Double currentBal = params.get("currentBalance") != null ? ((Number) params.get("currentBalance")).doubleValue() : 0.0;
                    Double monthlySavings = params.get("monthlySavingsRate") != null ? ((Number) params.get("monthlySavingsRate")).doubleValue() : 0.0;
                    Integer targetMonths = params.get("targetMonths") != null ? ((Number) params.get("targetMonths")).intValue() : 12;
                    Double returnPct = params.get("annualReturnPct") != null ? ((Number) params.get("annualReturnPct")).doubleValue() : 7.0;
                    Map<String, Object> res = toolService.simulateSavingsTrajectory(currentBal, monthlySavings, targetMonths, returnPct);
                    return objectMapper.writeValueAsString(res);
                }
                default:
                    return "{\"error\": \"Unknown tool: " + toolName + "\"}";
            }
        } catch (Exception e) {
            log.error("Error executing tool {}", toolName, e);
            return "{\"error\": \"" + e.getMessage() + "\"}";
        }
    }

    private void streamTokens(String text, Consumer<AgentStreamEvent> emitter) {
        if (text == null) return;
        String[] words = text.split("(?<=\\s)");
        for (String word : words) {
            emitter.accept(AgentStreamEvent.token(word));
            try {
                Thread.sleep(15); // Smooth streaming pacing
            } catch (InterruptedException ignored) {}
        }
    }

    private String callLlm(String prompt, int timeoutSec) {
        try {
            return CompletableFuture.supplyAsync(() -> chatModel.call(prompt))
                    .orTimeout(timeoutSec, TimeUnit.SECONDS)
                    .join();
        } catch (Exception e) {
            log.error("LLM call failed or timed out", e);
            return null;
        }
    }

    private String cleanModelOutput(String raw) {
        if (raw == null) return "";
        return raw.replaceAll("(?i)FINAL_ANSWER:\\s*", "").trim();
    }

    private String buildSystemPrompt(Long userId) {
        return """
                You are FinAgent, an elite autonomous Agentic AI Financial Advisor for the AI Finance Manager platform.
                You do NOT guess or hallucinate user financial numbers. Instead, you have access to live database tools to inspect transactions, aggregate spending, detect subscriptions, and simulate financial growth.

                AVAILABLE TOOLS:
                1. getMonthlySpendingByCategory: Takes {"month": 1-12, "year": 2026}. Returns exact category totals, income, expenses, and savings rate.
                2. getHistoricalTrends: Takes {"monthsCount": 6}. Returns month-over-month income, expense, and savings trends.
                3. getRecurringTransactions: Takes {}. Returns detected recurring charges, subscriptions, and flagged price increases.
                4. getBudgetVsActualReport: Takes {"month": 1-12, "year": 2026}. Compares user spending against 50/30/20 budget benchmarks.
                5. simulateSavingsTrajectory: Takes {"currentBalance": 1000, "monthlySavingsRate": 200, "targetMonths": 12, "annualReturnPct": 7.0}. Calculates future wealth trajectory.

                REASONING PROTOCOL:
                To answer questions, you MUST reason step-by-step using this format:

                THOUGHT: Explain what data you need or what step to take next.
                ACTION: <exact tool name>
                ACTION_INPUT: <JSON object containing input parameters>

                If you want to propose an action (e.g., creating a transaction, reallocating a budget) that modifies user data, output:
                PROPOSAL: {"actionType": "CREATE_TRANSACTION"|"REALLOCATE_BUDGET"|"BULK_RECATEGORIZE", "title": "Action Title", "description": "Details", "parameters": {"amount": 75.00, "description": "Olive Garden", "category": "Dining", "type": "EXPENSE"}}

                CATEGORY MATCHING RULE:
                When proposing a CREATE_TRANSACTION action, you MUST map the transaction to one of the user's existing categories provided in the context (e.g. choose "Dining" for restaurants/meals, "Groceries" for supermarket shopping, "Transport" for rides/gas, "Utilities" for bills). Never invent a new category name if an existing category fits!

                When you have gathered all necessary information and are ready to provide the complete answer to the user, output:
                FINAL_ANSWER: <Your complete, helpful, markdown-formatted response with bold numbers, bullet points, and tables if useful.>
                """;
    }
}
