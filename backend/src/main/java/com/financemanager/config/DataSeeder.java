package com.financemanager.config;

import com.financemanager.model.Category;
import com.financemanager.model.Transaction;
import com.financemanager.model.TransactionType;
import com.financemanager.model.User;
import com.financemanager.repository.CategoryRepository;
import com.financemanager.repository.TransactionRepository;
import com.financemanager.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final TransactionRepository transactionRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        log.info("Checking data seeder for test user 'shresth'...");

        User user = userRepository.findByUsername("shresth").orElseGet(() -> {
            log.info("Creating default test user 'shresth' (password: 1234)...");
            User newUser = User.builder()
                    .username("shresth")
                    .password(passwordEncoder.encode("1234"))
                    .firstName("Shresth")
                    .lastName("Developer")
                    .email("shresth@example.com")
                    .build();
            return userRepository.save(newUser);
        });

        // Ensure password is synchronized to '1234'
        if (!passwordEncoder.matches("1234", user.getPassword())) {
            user.setPassword(passwordEncoder.encode("1234"));
            userRepository.save(user);
        }

        seedCategoriesIfMissing(user);
        seedThreeMonthsTransactionsIfMissing(user);
    }

    private void seedCategoriesIfMissing(User user) {
        List<CategorySeed> defaultCategories = List.of(
                new CategorySeed("Salary", TransactionType.INCOME, "#10b981"),
                new CategorySeed("Freelance", TransactionType.INCOME, "#06b6d4"),
                new CategorySeed("Investments", TransactionType.INCOME, "#8b5cf6"),
                new CategorySeed("Rent", TransactionType.EXPENSE, "#ef4444"),
                new CategorySeed("Groceries", TransactionType.EXPENSE, "#f59e0b"),
                new CategorySeed("Dining", TransactionType.EXPENSE, "#ec4899"),
                new CategorySeed("Transport", TransactionType.EXPENSE, "#3b82f6"),
                new CategorySeed("Utilities", TransactionType.EXPENSE, "#6366f1"),
                new CategorySeed("Entertainment", TransactionType.EXPENSE, "#a855f7"),
                new CategorySeed("Subscriptions", TransactionType.EXPENSE, "#14b8a6"),
                new CategorySeed("Health", TransactionType.EXPENSE, "#84cc16"),
                new CategorySeed("Shopping", TransactionType.EXPENSE, "#f97316")
        );

        for (CategorySeed cs : defaultCategories) {
            if (!categoryRepository.existsByNameAndUserAndType(cs.name, user, cs.type)) {
                Category cat = Category.builder()
                        .name(cs.name)
                        .type(cs.type)
                        .color(cs.color)
                        .user(user)
                        .build();
                categoryRepository.save(cat);
            }
        }
    }

    private void seedThreeMonthsTransactionsIfMissing(User user) {
        List<Transaction> existing = transactionRepository.findAllByUserId(user.getId());
        if (!existing.isEmpty()) {
            log.info("User 'shresth' already has {} transactions. Skipping seeder.", existing.size());
            return;
        }

        log.info("Seeding 3 months of realistic mock transaction data for user 'shresth'...");
        List<Transaction> seedList = new ArrayList<>();
        LocalDate today = LocalDate.now();
        Random random = new Random(42);

        // Seed 3 months (e.g., monthOffset 2, 1, 0)
        for (int monthOffset = 2; monthOffset >= 0; monthOffset--) {
            YearMonth ym = YearMonth.from(today.minusMonths(monthOffset));
            int year = ym.getYear();
            int month = ym.getMonthValue();

            // 1. Monthly Salary (1st of month)
            BigDecimal salaryAmount = new BigDecimal("4800.00");
            seedList.add(Transaction.builder()
                    .user(user)
                    .amount(salaryAmount)
                    .description("Monthly Tech Salary")
                    .category("Salary")
                    .type(TransactionType.INCOME)
                    .date(LocalDate.of(year, month, 1))
                    .build());

            // 2. Freelance Bonus
            if (monthOffset == 1) {
                seedList.add(Transaction.builder()
                        .user(user)
                        .amount(new BigDecimal("850.00"))
                        .description("Freelance UI Project")
                        .category("Freelance")
                        .type(TransactionType.INCOME)
                        .date(LocalDate.of(year, month, 15))
                        .build());
            }

            // 3. Rent ($1,350 on the 3rd)
            seedList.add(Transaction.builder()
                    .user(user)
                    .amount(new BigDecimal("1350.00"))
                    .description("Apartment Monthly Rent")
                    .category("Rent")
                    .type(TransactionType.EXPENSE)
                    .date(LocalDate.of(year, month, 3))
                    .build());

            // 4. Utilities
            seedList.add(Transaction.builder()
                    .user(user)
                    .amount(new BigDecimal("124.50"))
                    .description("Electric & Water Utility Bill")
                    .category("Utilities")
                    .type(TransactionType.EXPENSE)
                    .date(LocalDate.of(year, month, 8))
                    .build());

            seedList.add(Transaction.builder()
                    .user(user)
                    .amount(new BigDecimal("79.99"))
                    .description("Fiber Internet Service")
                    .category("Utilities")
                    .type(TransactionType.EXPENSE)
                    .date(LocalDate.of(year, month, 12))
                    .build());

            // 5. Subscriptions (with a Price Hike on Netflix in the latest month!)
            BigDecimal netflixAmount = (monthOffset == 0) ? new BigDecimal("22.99") : new BigDecimal("15.99");
            seedList.add(Transaction.builder()
                    .user(user)
                    .amount(netflixAmount)
                    .description("Netflix Subscription")
                    .category("Subscriptions")
                    .type(TransactionType.EXPENSE)
                    .date(LocalDate.of(year, month, 14))
                    .build());

            seedList.add(Transaction.builder()
                    .user(user)
                    .amount(new BigDecimal("10.99"))
                    .description("Spotify Premium")
                    .category("Subscriptions")
                    .type(TransactionType.EXPENSE)
                    .date(LocalDate.of(year, month, 16))
                    .build());

            seedList.add(Transaction.builder()
                    .user(user)
                    .amount(new BigDecimal("20.00"))
                    .description("ChatGPT Plus Subscription")
                    .category("Subscriptions")
                    .type(TransactionType.EXPENSE)
                    .date(LocalDate.of(year, month, 19))
                    .build());

            seedList.add(Transaction.builder()
                    .user(user)
                    .amount(new BigDecimal("49.00"))
                    .description("Equinox Gym Membership")
                    .category("Health")
                    .type(TransactionType.EXPENSE)
                    .date(LocalDate.of(year, month, 5))
                    .build());

            // 6. Groceries
            int groceryDays[] = {4, 11, 18, 25};
            for (int day : groceryDays) {
                if (day <= ym.lengthOfMonth() && (monthOffset > 0 || day <= today.getDayOfMonth())) {
                    int cost = 75 + random.nextInt(45);
                    seedList.add(Transaction.builder()
                            .user(user)
                            .amount(new BigDecimal(cost + ".25"))
                            .description(random.nextBoolean() ? "Whole Foods Market" : "Trader Joe's Groceries")
                            .category("Groceries")
                            .type(TransactionType.EXPENSE)
                            .date(LocalDate.of(year, month, day))
                            .build());
                }
            }

            // 7. Dining & Outings
            int diningDays[] = {6, 13, 20, 27};
            for (int day : diningDays) {
                if (day <= ym.lengthOfMonth() && (monthOffset > 0 || day <= today.getDayOfMonth())) {
                    int cost = 32 + random.nextInt(40);
                    seedList.add(Transaction.builder()
                            .user(user)
                            .amount(new BigDecimal(cost + ".50"))
                            .description(random.nextBoolean() ? "Italian Bistro Dinner" : "Chipotle Mexican Grill")
                            .category("Dining")
                            .type(TransactionType.EXPENSE)
                            .date(LocalDate.of(year, month, day))
                            .build());
                }
            }

            // 8. Transport
            int transportDays[] = {9, 22};
            for (int day : transportDays) {
                if (day <= ym.lengthOfMonth() && (monthOffset > 0 || day <= today.getDayOfMonth())) {
                    int cost = 24 + random.nextInt(30);
                    seedList.add(Transaction.builder()
                            .user(user)
                            .amount(new BigDecimal(cost + ".00"))
                            .description(random.nextBoolean() ? "Uber Ride" : "Shell Gas Station")
                            .category("Transport")
                            .type(TransactionType.EXPENSE)
                            .date(LocalDate.of(year, month, day))
                            .build());
                }
            }

            // 9. Entertainment
            if (monthOffset != 0 || today.getDayOfMonth() >= 23) {
                seedList.add(Transaction.builder()
                        .user(user)
                        .amount(new BigDecimal("65.00"))
                        .description("AMC Movie Theater & Popcorn")
                        .category("Entertainment")
                        .type(TransactionType.EXPENSE)
                        .date(LocalDate.of(year, month, Math.min(23, ym.lengthOfMonth())))
                        .build());
            }
        }

        transactionRepository.saveAll(seedList);
        log.info("Successfully seeded {} mock transactions spanning 3 months for 'shresth'!", seedList.size());
    }

    private record CategorySeed(String name, TransactionType type, String color) {}
}
