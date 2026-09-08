package com.financemanager.service;

import com.financemanager.model.Transaction;
import com.financemanager.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class AIService {

    private final ChatModel chatModel;
    private final TransactionRepository transactionRepository;

    public String categorizeTransaction(String description, String amount) {
        String prompt = "Categorize this transaction description into a single short category (e.g., Food, Transport, Rent, Salary, Utilities, Shopping, Entertainment, Health). Description: \""
                + description + "\", Amount: " + amount + ". Respond ONLY with the category name, nothing else.";
        return callModelWithTimeout(prompt, "Uncategorized", 60); // 60-second timeout for background tasks
    }

    public String getFinancialAdvice(String userContext) {
        String prompt = "You are a helpful financial advisor. Based on this summary of transactions, give a brief financial tip: "
                + userContext;
        return callModelWithTimeout(prompt, "No advice available at the moment. Please try again later.", 60); // 1-minute timeout for advice
    }

    public String chat(String prompt) {
        return callModelWithTimeout(prompt, "I'm sorry, I'm having trouble responding right now. Please try again.", 60); // 1-minute timeout for chat
    }

    @Async
    public void categorizeTransactionAsync(Long transactionId, String description, String amount) {
        System.out.println("Starting async categorization for transaction ID: " + transactionId);
        
        String prompt = "Categorize this transaction description into a single short category (e.g., Food, Transport, Rent, Salary, Utilities, Shopping, Entertainment, Health). Description: \""
                + description + "\", Amount: " + amount + ". Respond ONLY with the category name, nothing else.";
        
        String aiResponse = callModelWithTimeout(prompt, "Uncategorized", 30); // 30-second timeout
        String sanitizedCategory = sanitizeCategory(aiResponse);
        
        transactionRepository.findById(transactionId).ifPresent(transaction -> {
            transaction.setCategory(sanitizedCategory);
            transactionRepository.save(transaction);
            System.out.println("Async categorization completed for transaction ID: " + transactionId + ". Resolved Category: " + sanitizedCategory);
        });
    }

    private String callModelWithTimeout(String prompt, String defaultFallback, int timeoutSeconds) {
        try {
            System.out.println("Calling Spring AI ChatModel with prompt: " + prompt);
            return CompletableFuture.supplyAsync(() -> chatModel.call(prompt))
                    .orTimeout(timeoutSeconds, TimeUnit.SECONDS)
                    .join();
        } catch (Exception e) {
            System.err.println("Error calling AI model or timed out: " + e.getMessage());
            return defaultFallback;
        }
    }

    private String sanitizeCategory(String aiResponse) {
        if (aiResponse == null || aiResponse.trim().isEmpty()) {
            return "Uncategorized";
        }
        String responseLower = aiResponse.trim().toLowerCase();
        List<String> validCategories = List.of(
            "food", "transport", "rent", "salary", "utilities", "shopping", "entertainment", "health"
        );
        for (String validCat : validCategories) {
            if (responseLower.contains(validCat)) {
                return validCat.substring(0, 1).toUpperCase() + validCat.substring(1);
            }
        }
        return "Uncategorized";
    }
}
