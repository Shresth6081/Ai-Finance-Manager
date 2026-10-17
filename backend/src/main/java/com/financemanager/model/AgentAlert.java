package com.financemanager.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
    name = "agent_alerts",
    indexes = {
        @Index(name = "idx_alert_user_dismissed", columnList = "user_id, is_dismissed, created_at")
    }
)
public class AgentAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "alert_type", nullable = false, length = 50)
    private String alertType; // e.g., 'PRICE_HIKE', 'BUDGET_OVERRUN', 'SAVINGS_OPPORTUNITY', 'UNUSUAL_SPEND'

    @Column(nullable = false, length = 255)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(name = "suggested_action", columnDefinition = "TEXT")
    private String suggestedAction; // JSON string payload for actionable proposals

    @Builder.Default
    @Column(name = "is_dismissed", nullable = false)
    private boolean isDismissed = false;

    @Builder.Default
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
