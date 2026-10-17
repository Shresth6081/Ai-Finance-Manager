package com.financemanager.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentStreamEvent {
    private String type; // 'thought', 'tool_call', 'tool_result', 'token', 'action_required', 'done', 'error'
    private String message;
    private String toolName;
    private Object payload;
    private Long timestamp;

    public static AgentStreamEvent thought(String message) {
        return AgentStreamEvent.builder()
                .type("thought")
                .message(message)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    public static AgentStreamEvent toolCall(String toolName, Object inputParams, String message) {
        return AgentStreamEvent.builder()
                .type("tool_call")
                .toolName(toolName)
                .payload(inputParams)
                .message(message)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    public static AgentStreamEvent toolResult(String toolName, Object result) {
        return AgentStreamEvent.builder()
                .type("tool_result")
                .toolName(toolName)
                .payload(result)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    public static AgentStreamEvent token(String text) {
        return AgentStreamEvent.builder()
                .type("token")
                .message(text)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    public static AgentStreamEvent actionRequired(AgentActionProposal proposal) {
        return AgentStreamEvent.builder()
                .type("action_required")
                .payload(proposal)
                .message("Action requires human confirmation.")
                .timestamp(System.currentTimeMillis())
                .build();
    }

    public static AgentStreamEvent done(String finalMessage) {
        return AgentStreamEvent.builder()
                .type("done")
                .message(finalMessage)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    public static AgentStreamEvent error(String errorMessage) {
        return AgentStreamEvent.builder()
                .type("error")
                .message(errorMessage)
                .timestamp(System.currentTimeMillis())
                .build();
    }
}
