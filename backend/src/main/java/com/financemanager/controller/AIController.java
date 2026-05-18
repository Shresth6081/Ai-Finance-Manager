package com.financemanager.controller;

import com.financemanager.service.AIService;
import com.financemanager.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
public class AIController {

    private final AIService aiService;
    private final TransactionService transactionService;

    @GetMapping("/advice")
    public ResponseEntity<String> getFinancialAdvice() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = ((UserDetails) authentication.getPrincipal()).getUsername();

        // Fetch recent transactions to give context to AI
        var transactions = transactionService.getTransactions(username);
        String context = transactions.stream()
                .map(t -> t.getType() + ": " + t.getDescription() + " - $" + t.getAmount())
                .collect(Collectors.joining("; "));

        if (context.isEmpty()) {
            return ResponseEntity.ok("No transactions found. add some income and expenses to get advice!");
        }

        return ResponseEntity.ok(aiService.getFinancialAdvice(context));
    }
}
