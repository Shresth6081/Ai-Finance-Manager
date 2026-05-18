package com.financemanager.controller;

import com.financemanager.dto.TransactionDTO;
import com.financemanager.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping
    public ResponseEntity<TransactionDTO> createTransaction(@RequestBody TransactionDTO dto) {
        String username = getUsername();
        return ResponseEntity.ok(transactionService.createTransaction(dto, username));
    }

    @GetMapping
    public ResponseEntity<List<TransactionDTO>> getTransactions() {
        String username = getUsername();
        return ResponseEntity.ok(transactionService.getTransactions(username));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TransactionDTO> updateTransaction(
            @PathVariable Long id,
            @RequestBody TransactionDTO dto) {
        String username = getUsername();
        return ResponseEntity.ok(transactionService.updateTransaction(id, dto, username));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTransaction(@PathVariable Long id) {
        String username = getUsername();
        transactionService.deleteTransaction(id, username);
        return ResponseEntity.noContent().build();
    }

    private String getUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return ((UserDetails) authentication.getPrincipal()).getUsername();
    }
}
