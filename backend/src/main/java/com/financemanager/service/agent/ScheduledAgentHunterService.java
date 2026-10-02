package com.financemanager.service.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.financemanager.model.AgentAlert;
import com.financemanager.model.User;
import com.financemanager.repository.AgentAlertRepository;
import com.financemanager.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduledAgentHunterService {

    private final UserRepository userRepository;
    private final AgentAlertRepository alertRepository;
    private final FinancialToolService toolService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Runs every 6 hours and also 15 seconds after app startup.
     */
    @Scheduled(initialDelay = 15000, fixedRate = 6 * 60 * 60 * 1000)
    @Transactional
    public void runAutonomousHunterScan() {
        log.info("Starting autonomous financial hunter scan across all users...");
        List<User> users = userRepository.findAll();

        for (User user : users) {
            try {
                scanUserForSubscriptionSpikes(user);
                scanUserForBudgetVelocity(user);
            } catch (Exception e) {
                log.error("Error running hunter scan for user {}", user.getUsername(), e);
            }
        }
        log.info("Autonomous financial hunter scan complete.");
    }

    private void scanUserForSubscriptionSpikes(User user) {
        Map<String, Object> recurringData = toolService.getRecurringTransactions(user.getId());
        List<Map<String, Object>> subs = (List<Map<String, Object>>) recurringData.get("subscriptions");

        if (subs == null || subs.isEmpty()) return;

        for (Map<String, Object> sub : subs) {
            boolean isHike = Boolean.TRUE.equals(sub.get("isPriceHike"));
            Double changePct = (Double) sub.get("priceChangePercentage");
            String vendor = (String) sub.get("vendor");
            BigDecimal latest = (BigDecimal) sub.get("latestAmount");
            BigDecimal prev = (BigDecimal) sub.get("previousAmount");

            if (isHike && changePct != null && changePct > 0) {
                String title = "Price Increase: " + vendor;
                if (!hasRecentAlert(user.getId(), title)) {
                    Map<String, Object> actionProposal = Map.of(
                            "actionType", "REVIEW_SUBSCRIPTION",
                            "vendor", vendor,
                            "latestAmount", latest,
                            "previousAmount", prev
                    );

                    AgentAlert alert = AgentAlert.builder()
                            .user(user)
                            .alertType("PRICE_HIKE")
                            .title(title)
                            .message(String.format("We noticed %s increased by %.1f%% (from $%s to $%s). Consider reviewing or renegotiating this plan.", vendor, changePct, prev, latest))
                            .suggestedAction(toJson(actionProposal))
                            .createdAt(LocalDateTime.now())
                            .build();

                    alertRepository.save(alert);
                    log.info("Generated PRICE_HIKE alert for user {}: {}", user.getUsername(), title);
                }
            }
        }
    }

    private void scanUserForBudgetVelocity(User user) {
        LocalDate now = LocalDate.now();
        Map<String, Object> spendData = toolService.getMonthlySpendingByCategory(user.getId(), now.getMonthValue(), now.getYear());
        BigDecimal totalIncome = (BigDecimal) spendData.get("totalIncome");
        BigDecimal totalExpense = (BigDecimal) spendData.get("totalExpense");
        Double savingsRate = (Double) spendData.get("savingsRatePercentage");

        // If high surplus savings rate in current month
        if (savingsRate != null && savingsRate >= 30.0 && totalIncome.compareTo(new BigDecimal("500")) > 0) {
            String title = "Strong Savings Rate (" + savingsRate + "%)";
            if (!hasRecentAlert(user.getId(), title)) {
                AgentAlert alert = AgentAlert.builder()
                        .user(user)
                        .alertType("SAVINGS_OPPORTUNITY")
                        .title(title)
                        .message("You have retained " + savingsRate + "% of your earnings this month! You could allocate a portion to compound investments.")
                        .suggestedAction(toJson(Map.of("actionType", "INVEST_SURPLUS", "savingsRate", savingsRate)))
                        .createdAt(LocalDateTime.now())
                        .build();

                alertRepository.save(alert);
            }
        }
    }

    private boolean hasRecentAlert(Long userId, String title) {
        List<AgentAlert> existing = alertRepository.findAllByUserId(userId);
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
        return existing.stream()
                .anyMatch(a -> a.getTitle().equalsIgnoreCase(title) && a.getCreatedAt().isAfter(sevenDaysAgo));
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return "{}";
        }
    }
}
