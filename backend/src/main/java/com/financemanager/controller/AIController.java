package com.financemanager.controller;

import com.financemanager.service.AIService;
import com.financemanager.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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

    @PostMapping("/chat")
    public ResponseEntity<String> chat(@RequestBody ChatRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = ((UserDetails) authentication.getPrincipal()).getUsername();

        // Fetch recent transactions to give context to AI (limit to most recent 15 to prevent Ollama slowness/timeout)
        var transactions = transactionService.getTransactions(username);
        String context = transactions.stream()
                .limit(15)
                .map(t -> t.getType() + ": " + t.getDescription() + " - $" + t.getAmount() + " on " + t.getDate() + " (" + t.getCategory() + ")")
                .collect(Collectors.joining("; "));

        String prompt = "You are FinManager AI, a helpful personal finance assistant. "
                + "Below is the user's recent financial transactions context:\n"
                + "[" + (context.isEmpty() ? "No transaction history yet." : context) + "]\n\n"
                + "User's message: \"" + request.getMessage() + "\"\n"
                + "Provide a helpful, friendly, and concise response. "
                + "If they ask general questions (about their finances), feel free to answer normally.";

        return ResponseEntity.ok(aiService.chat(prompt));
    }

    static class ChatRequest {
        private String message;

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }
}
