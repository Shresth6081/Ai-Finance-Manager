package com.financemanager.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.financemanager.service.agent.FinancialToolService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Description;

import java.util.Map;
import java.util.function.Function;

@Configuration
@RequiredArgsConstructor
public class AgentToolsConfig {

    private final FinancialToolService toolService;

    public record SpendingBreakdownRequest(
            @JsonProperty(required = true) @JsonPropertyDescription("User ID to query") Long userId,
            @JsonPropertyDescription("Month number (1-12)") Integer month,
            @JsonPropertyDescription("Year (e.g. 2026)") Integer year
    ) {}

    public record TrendsRequest(
            @JsonProperty(required = true) @JsonPropertyDescription("User ID to query") Long userId,
            @JsonPropertyDescription("Number of months (e.g. 6)") Integer monthsCount
    ) {}

    public record RecurringRequest(
            @JsonProperty(required = true) @JsonPropertyDescription("User ID to query") Long userId
    ) {}

    public record BudgetReportRequest(
            @JsonProperty(required = true) @JsonPropertyDescription("User ID to query") Long userId,
            @JsonPropertyDescription("Month number (1-12)") Integer month,
            @JsonPropertyDescription("Year (e.g. 2026)") Integer year
    ) {}

    public record SimulationRequest(
            @JsonPropertyDescription("Current starting balance") Double currentBalance,
            @JsonPropertyDescription("Monthly deposit or savings rate") Double monthlySavingsRate,
            @JsonPropertyDescription("Number of target months (e.g. 12)") Integer targetMonths,
            @JsonPropertyDescription("Annual return percentage (e.g. 7.0)") Double annualReturnPct
    ) {}

    @Bean
    @Description("Get detailed category spending breakdown, income, expense, and savings rate for a specific month and year")
    public Function<SpendingBreakdownRequest, Map<String, Object>> getMonthlySpendingByCategory() {
        return req -> toolService.getMonthlySpendingByCategory(req.userId(), req.month(), req.year());
    }

    @Bean
    @Description("Get historical income, expenses, and savings trends over the last N months")
    public Function<TrendsRequest, Map<String, Object>> getHistoricalTrends() {
        return req -> toolService.getHistoricalTrends(req.userId(), req.monthsCount());
    }

    @Bean
    @Description("Detect recurring charges, subscriptions, and identify price increases")
    public Function<RecurringRequest, Map<String, Object>> getRecurringTransactions() {
        return req -> toolService.getRecurringTransactions(req.userId());
    }

    @Bean
    @Description("Compare user spending against 50/30/20 budget benchmarks and analyze category limits")
    public Function<BudgetReportRequest, Map<String, Object>> getBudgetVsActualReport() {
        return req -> toolService.getBudgetVsActualReport(req.userId(), req.month(), req.year());
    }

    @Bean
    @Description("Simulate savings growth trajectory, wealth projection, and compound interest milestones")
    public Function<SimulationRequest, Map<String, Object>> simulateSavingsTrajectory() {
        return req -> toolService.simulateSavingsTrajectory(
                req.currentBalance(), req.monthlySavingsRate(), req.targetMonths(), req.annualReturnPct()
        );
    }
}
