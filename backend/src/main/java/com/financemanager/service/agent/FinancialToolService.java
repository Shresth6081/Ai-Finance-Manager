package com.financemanager.service.agent;

import com.financemanager.dto.AgentActionProposal;
import com.financemanager.model.Category;
import com.financemanager.model.Transaction;
import com.financemanager.model.TransactionType;
import com.financemanager.model.User;
import com.financemanager.repository.CategoryRepository;
import com.financemanager.repository.TransactionRepository;
import com.financemanager.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FinancialToolService {

    private final TransactionRepository transactionRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    public Map<String, Object> getMonthlySpendingByCategory(Long userId, Integer month, Integer year) {
        LocalDate now = LocalDate.now();
        int targetYear = (year != null && year > 2000) ? year : now.getYear();
        int targetMonth = (month != null && month >= 1 && month <= 12) ? month : now.getMonthValue();

        YearMonth yearMonth = YearMonth.of(targetYear, targetMonth);
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();

        List<Transaction> transactions = transactionRepository.findByUserIdAndDateBetween(userId, startDate, endDate);

        BigDecimal totalIncome = BigDecimal.ZERO;
        BigDecimal totalExpense = BigDecimal.ZERO;
        Map<String, BigDecimal> expenseByCategory = new HashMap<>();
        Map<String, Integer> countByCategory = new HashMap<>();

        for (Transaction t : transactions) {
            BigDecimal amount = t.getAmount() != null ? t.getAmount() : BigDecimal.ZERO;
            if (t.getType() == TransactionType.INCOME) {
                totalIncome = totalIncome.add(amount);
            } else {
                totalExpense = totalExpense.add(amount);
                String cat = (t.getCategory() != null && !t.getCategory().isBlank()) ? t.getCategory() : "Uncategorized";
                expenseByCategory.put(cat, expenseByCategory.getOrDefault(cat, BigDecimal.ZERO).add(amount));
                countByCategory.put(cat, countByCategory.getOrDefault(cat, 0) + 1);
            }
        }

        BigDecimal netSavings = totalIncome.subtract(totalExpense);
        double savingsRate = totalIncome.compareTo(BigDecimal.ZERO) > 0
                ? (netSavings.doubleValue() / totalIncome.doubleValue()) * 100.0
                : 0.0;

        List<Map<String, Object>> categoryList = new ArrayList<>();
        final BigDecimal finalTotalExpense = totalExpense;
        expenseByCategory.entrySet().stream()
                .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed())
                .forEach(entry -> {
                    Map<String, Object> catMap = new HashMap<>();
                    catMap.put("category", entry.getKey());
                    catMap.put("amount", entry.getValue());
                    catMap.put("transactionCount", countByCategory.getOrDefault(entry.getKey(), 0));
                    double pct = finalTotalExpense.compareTo(BigDecimal.ZERO) > 0
                            ? (entry.getValue().doubleValue() / finalTotalExpense.doubleValue()) * 100.0
                            : 0.0;
                    catMap.put("percentageOfExpenses", Math.round(pct * 10.0) / 10.0);
                    categoryList.add(catMap);
                });

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("period", yearMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")));
        result.put("month", targetMonth);
        result.put("year", targetYear);
        result.put("totalIncome", totalIncome);
        result.put("totalExpense", totalExpense);
        result.put("netSavings", netSavings);
        result.put("savingsRatePercentage", Math.round(savingsRate * 10.0) / 10.0);
        result.put("totalTransactionsCount", transactions.size());
        result.put("categories", categoryList);

        return result;
    }

    public Map<String, Object> getHistoricalTrends(Long userId, Integer monthsCount) {
        int count = (monthsCount != null && monthsCount >= 1 && monthsCount <= 24) ? monthsCount : 6;
        LocalDate now = LocalDate.now();
        LocalDate startDate = now.minusMonths(count - 1).withDayOfMonth(1);
        LocalDate endDate = now.withDayOfMonth(now.lengthOfMonth());

        List<Transaction> transactions = transactionRepository.findByUserIdAndDateBetween(userId, startDate, endDate);

        // Group by YearMonth
        Map<YearMonth, List<Transaction>> monthlyGroups = transactions.stream()
                .collect(Collectors.groupingBy(t -> YearMonth.from(t.getDate())));

        List<Map<String, Object>> trendList = new ArrayList<>();
        YearMonth cursor = YearMonth.from(startDate);
        YearMonth endYm = YearMonth.from(endDate);

        while (!cursor.isAfter(endYm)) {
            List<Transaction> monthTxns = monthlyGroups.getOrDefault(cursor, Collections.emptyList());

            BigDecimal income = BigDecimal.ZERO;
            BigDecimal expense = BigDecimal.ZERO;
            Map<String, BigDecimal> catSpend = new HashMap<>();

            for (Transaction t : monthTxns) {
                BigDecimal amt = t.getAmount() != null ? t.getAmount() : BigDecimal.ZERO;
                if (t.getType() == TransactionType.INCOME) {
                    income = income.add(amt);
                } else {
                    expense = expense.add(amt);
                    String cat = (t.getCategory() != null && !t.getCategory().isBlank()) ? t.getCategory() : "Uncategorized";
                    catSpend.put(cat, catSpend.getOrDefault(cat, BigDecimal.ZERO).add(amt));
                }
            }

            String topCategory = catSpend.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse("None");

            BigDecimal net = income.subtract(expense);
            double savingsRate = income.compareTo(BigDecimal.ZERO) > 0
                    ? (net.doubleValue() / income.doubleValue()) * 100.0
                    : 0.0;

            Map<String, Object> monthData = new LinkedHashMap<>();
            monthData.put("month", cursor.format(DateTimeFormatter.ofPattern("MMM yyyy")));
            monthData.put("income", income);
            monthData.put("expense", expense);
            monthData.put("netSavings", net);
            monthData.put("savingsRatePercentage", Math.round(savingsRate * 10.0) / 10.0);
            monthData.put("topExpenseCategory", topCategory);

            trendList.add(monthData);
            cursor = cursor.plusMonths(1);
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("rangeMonths", count);
        response.put("trends", trendList);
        return response;
    }

    public Map<String, Object> getRecurringTransactions(Long userId) {
        LocalDate startDate = LocalDate.now().minusMonths(6);
        List<Transaction> transactions = transactionRepository.findByUserIdAndDateBetween(userId, startDate, LocalDate.now());

        // Group expense transactions by normalized description/vendor
        Map<String, List<Transaction>> vendorGroups = transactions.stream()
                .filter(t -> t.getType() == TransactionType.EXPENSE)
                .collect(Collectors.groupingBy(t -> normalizeVendorName(t.getDescription())));

        List<Map<String, Object>> recurringList = new ArrayList<>();

        for (Map.Entry<String, List<Transaction>> entry : vendorGroups.entrySet()) {
            List<Transaction> list = entry.getValue();
            if (list.size() >= 2) { // Appears at least twice in 6 months
                list.sort(Comparator.comparing(Transaction::getDate));

                Transaction latest = list.get(list.size() - 1);
                Transaction previous = list.get(list.size() - 2);

                BigDecimal latestAmt = latest.getAmount();
                BigDecimal prevAmt = previous.getAmount();
                BigDecimal diff = latestAmt.subtract(prevAmt);

                double priceChangePct = prevAmt.compareTo(BigDecimal.ZERO) > 0
                        ? (diff.doubleValue() / prevAmt.doubleValue()) * 100.0
                        : 0.0;

                Map<String, Object> item = new LinkedHashMap<>();
                item.put("vendor", entry.getKey());
                item.put("category", latest.getCategory());
                item.put("occurrencesCount", list.size());
                item.put("latestChargeDate", latest.getDate().toString());
                item.put("latestAmount", latestAmt);
                item.put("previousAmount", prevAmt);
                item.put("priceChangePercentage", Math.round(priceChangePct * 10.0) / 10.0);
                item.put("isPriceHike", diff.compareTo(BigDecimal.ZERO) > 0);

                recurringList.add(item);
            }
        }

        recurringList.sort((a, b) -> ((BigDecimal) b.get("latestAmount")).compareTo((BigDecimal) a.get("latestAmount")));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalRecurringIdentified", recurringList.size());
        result.put("subscriptions", recurringList);
        return result;
    }

    public Map<String, Object> getBudgetVsActualReport(Long userId, Integer month, Integer year) {
        Map<String, Object> spendData = getMonthlySpendingByCategory(userId, month, year);
        BigDecimal totalIncome = (BigDecimal) spendData.get("totalIncome");
        BigDecimal totalExpense = (BigDecimal) spendData.get("totalExpense");

        // 50/30/20 rule benchmarks
        BigDecimal targetNeeds = totalIncome.multiply(new BigDecimal("0.50")).setScale(2, RoundingMode.HALF_UP);
        BigDecimal targetWants = totalIncome.multiply(new BigDecimal("0.30")).setScale(2, RoundingMode.HALF_UP);
        BigDecimal targetSavings = totalIncome.multiply(new BigDecimal("0.20")).setScale(2, RoundingMode.HALF_UP);

        BigDecimal actualSavings = totalIncome.subtract(totalExpense);

        Map<String, Object> benchmarks = new LinkedHashMap<>();
        benchmarks.put("targetNeeds_50pct", targetNeeds);
        benchmarks.put("targetWants_30pct", targetWants);
        benchmarks.put("targetSavings_20pct", targetSavings);
        benchmarks.put("actualSavings", actualSavings);
        benchmarks.put("isMeetingSavingsTarget", actualSavings.compareTo(targetSavings) >= 0);

        Map<String, Object> report = new LinkedHashMap<>(spendData);
        report.put("budgetBenchmarks", benchmarks);
        return report;
    }

    public Map<String, Object> simulateSavingsTrajectory(Double currentBalance, Double monthlySavingsRate, Integer targetMonths, Double annualReturnPct) {
        double balance = (currentBalance != null && currentBalance >= 0) ? currentBalance : 0.0;
        double monthlyDeposit = (monthlySavingsRate != null) ? monthlySavingsRate : 0.0;
        int months = (targetMonths != null && targetMonths > 0 && targetMonths <= 360) ? targetMonths : 12;
        double annualReturn = (annualReturnPct != null && annualReturnPct >= 0) ? annualReturnPct : 7.0; // default 7% S&P500
        double monthlyInterestRate = (annualReturn / 100.0) / 12.0;

        List<Map<String, Object>> milestones = new ArrayList<>();
        double runningBalance = balance;
        double totalDeposited = balance;

        for (int m = 1; m <= months; m++) {
            runningBalance += monthlyDeposit;
            totalDeposited += monthlyDeposit;
            runningBalance += (runningBalance * monthlyInterestRate);

            if (m == 3 || m == 6 || m == 12 || m == 24 || m == 36 || m == 60 || m == months) {
                Map<String, Object> ms = new LinkedHashMap<>();
                ms.put("month", m);
                ms.put("projectedBalance", Math.round(runningBalance * 100.0) / 100.0);
                ms.put("totalDeposits", Math.round(totalDeposited * 100.0) / 100.0);
                ms.put("totalInterestEarned", Math.round((runningBalance - totalDeposited) * 100.0) / 100.0);
                milestones.add(ms);
            }
        }

        Map<String, Object> simulation = new LinkedHashMap<>();
        simulation.put("initialBalance", balance);
        simulation.put("monthlyContribution", monthlyDeposit);
        simulation.put("durationMonths", months);
        simulation.put("assumedAnnualReturnPct", annualReturn);
        simulation.put("finalProjectedBalance", Math.round(runningBalance * 100.0) / 100.0);
        simulation.put("totalInterestEarned", Math.round((runningBalance - totalDeposited) * 100.0) / 100.0);
        simulation.put("milestoneBreakdown", milestones);

        return simulation;
    }

    @Transactional
    public Map<String, Object> executeActionProposal(Long userId, AgentActionProposal proposal) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        String actionType = proposal.getActionType();
        Map<String, Object> params = proposal.getParameters() != null ? proposal.getParameters() : Collections.emptyMap();
        Map<String, Object> executionResult = new LinkedHashMap<>();

        log.info("Executing approved agent action: {} for user: {}", actionType, user.getUsername());

        switch (actionType != null ? actionType.toUpperCase() : "") {
            case "BULK_RECATEGORIZE": {
                String newCategory = (String) params.get("newCategory");
                Object txnIdsObj = params.get("transactionIds");
                List<Long> ids = new ArrayList<>();

                if (txnIdsObj instanceof List<?> rawList) {
                    for (Object item : rawList) {
                        if (item instanceof Number n) {
                            ids.add(n.longValue());
                        } else if (item != null) {
                            ids.add(Long.parseLong(item.toString()));
                        }
                    }
                }

                if (ids.isEmpty() || newCategory == null || newCategory.isBlank()) {
                    throw new IllegalArgumentException("Invalid parameters for BULK_RECATEGORIZE");
                }

                int updatedCount = 0;
                for (Long txnId : ids) {
                    Optional<Transaction> txnOpt = transactionRepository.findById(txnId);
                    if (txnOpt.isPresent() && txnOpt.get().getUser().getId().equals(userId)) {
                        Transaction txn = txnOpt.get();
                        txn.setCategory(newCategory.trim());
                        transactionRepository.save(txn);
                        updatedCount++;
                    }
                }

                executionResult.put("status", "SUCCESS");
                executionResult.put("action", "BULK_RECATEGORIZE");
                executionResult.put("updatedCount", updatedCount);
                executionResult.put("newCategory", newCategory);
                executionResult.put("message", "Successfully updated " + updatedCount + " transactions to category '" + newCategory + "'");
                break;
            }

            case "CREATE_TRANSACTION": {
                BigDecimal amount = new BigDecimal(params.get("amount").toString());
                String description = (String) params.get("description");
                if (description == null || description.isBlank()) {
                    description = (String) params.get("merchant");
                }
                if (description == null || description.isBlank()) {
                    description = (String) params.get("title");
                }
                if (description == null || description.isBlank()) {
                    description = "New Expense";
                }

                String rawCategory = (String) params.getOrDefault("category", "General");
                List<Category> userCategories = categoryRepository.findByUser(user);
                String category = userCategories.stream()
                        .filter(c -> c.getName().equalsIgnoreCase(rawCategory.trim()))
                        .map(Category::getName)
                        .findFirst()
                        .orElse(rawCategory.trim());

                String typeStr = (String) params.getOrDefault("type", "EXPENSE");
                TransactionType type = TransactionType.valueOf(typeStr.toUpperCase());
                String dateStr = (String) params.getOrDefault("date", LocalDate.now().toString());
                LocalDate date = LocalDate.parse(dateStr);

                Transaction txn = Transaction.builder()
                        .user(user)
                        .amount(amount)
                        .description(description)
                        .category(category)
                        .type(type)
                        .date(date)
                        .build();

                Transaction saved = transactionRepository.save(txn);
                executionResult.put("status", "SUCCESS");
                executionResult.put("action", "CREATE_TRANSACTION");
                executionResult.put("transactionId", saved.getId());
                executionResult.put("message", "Created transaction '" + description + "' for $" + amount);
                break;
            }

            case "CREATE_CATEGORY": {
                String name = (String) params.get("name");
                String typeStr = (String) params.getOrDefault("type", "EXPENSE");
                TransactionType type = TransactionType.valueOf(typeStr.toUpperCase());
                String color = (String) params.getOrDefault("color", "#3b82f6");

                if (!categoryRepository.existsByNameAndUserAndType(name, user, type)) {
                    Category cat = Category.builder()
                            .name(name)
                            .type(type)
                            .color(color)
                            .user(user)
                            .build();
                    categoryRepository.save(cat);
                    executionResult.put("status", "SUCCESS");
                    executionResult.put("message", "Created category '" + name + "'");
                } else {
                    executionResult.put("status", "ALREADY_EXISTS");
                    executionResult.put("message", "Category '" + name + "' already exists");
                }
                break;
            }

            case "REALLOCATE_BUDGET": {
                String fromCat = (String) params.get("fromCategory");
                String toCat = (String) params.get("toCategory");
                String amtStr = params.get("amount") != null ? params.get("amount").toString() : "0";

                executionResult.put("status", "SUCCESS");
                executionResult.put("action", "REALLOCATE_BUDGET");
                executionResult.put("message", "Budget reallocated: $" + amtStr + " shifted from " + fromCat + " to " + toCat);
                break;
            }

            default:
                throw new IllegalArgumentException("Unknown or unsupported action type: " + actionType);
        }

        return executionResult;
    }

    private String normalizeVendorName(String desc) {
        if (desc == null || desc.isBlank()) return "Unknown Vendor";
        String clean = desc.trim().toLowerCase()
                .replaceAll("\\b(inc|llc|ltd|co|corp|payment|autopay|subscription|bill)\\b", "")
                .replaceAll("[^a-zA-Z0-9 ]", "")
                .trim();
        if (clean.isEmpty()) return desc.trim();
        return Character.toUpperCase(clean.charAt(0)) + clean.substring(1);
    }
}
