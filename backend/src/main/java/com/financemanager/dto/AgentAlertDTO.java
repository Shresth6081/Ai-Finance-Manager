package com.financemanager.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentAlertDTO {
    private Long id;
    private String alertType;
    private String title;
    private String message;
    private String suggestedAction;
    private boolean isDismissed;
    private LocalDateTime createdAt;
}
