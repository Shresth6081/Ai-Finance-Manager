package com.financemanager.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentActionProposal {
    private String actionId;
    private String actionType; // 'REALLOCATE_BUDGET', 'BULK_RECATEGORIZE', 'CREATE_TRANSACTION', 'CREATE_GOAL'
    private String title;
    private String description;
    private Map<String, Object> parameters; // e.g. { "fromCategory": "Entertainment", "toCategory": "Savings", "amount": 100 }
    @Builder.Default
    private String status = "PENDING"; // PENDING, APPROVED, REJECTED, EXECUTED, FAILED
}
