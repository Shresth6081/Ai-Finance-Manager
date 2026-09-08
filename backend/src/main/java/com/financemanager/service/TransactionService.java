package com.financemanager.service;

import com.financemanager.dto.TransactionDTO;
import com.financemanager.model.Transaction;
import com.financemanager.model.User;
import com.financemanager.repository.TransactionRepository;
import com.financemanager.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final AIService aiService;

    public TransactionDTO createTransaction(TransactionDTO dto, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        String category = dto.getCategory();
        boolean needsAsyncCategorization = false;
        if (category == null || category.trim().isEmpty()) {
            category = "Pending";
            needsAsyncCategorization = true;
        }

        Transaction transaction = Transaction.builder()
                .amount(dto.getAmount())
                .description(dto.getDescription())
                .date(dto.getDate())
                .type(dto.getType())
                .category(category)
                .user(user)
                .build();

        Transaction savedTransaction = transactionRepository.save(transaction);

        if (needsAsyncCategorization) {
            aiService.categorizeTransactionAsync(savedTransaction.getId(), savedTransaction.getDescription(), savedTransaction.getAmount().toString());
        }

        return mapToDTO(savedTransaction);
    }

    public List<TransactionDTO> getTransactions(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        return transactionRepository.findAllByUserId(user.getId()).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public TransactionDTO updateTransaction(Long id, TransactionDTO dto, String username) {
        Transaction transaction = transactionRepository
                .findById(Objects.requireNonNull(id, "Transaction id must not be null"))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Transaction not found"));

        // Ownership check — users can only edit their own transactions
        if (!transaction.getUser().getUsername().equals(username)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }

        transaction.setAmount(dto.getAmount());
        transaction.setDescription(dto.getDescription());
        transaction.setDate(dto.getDate());
        transaction.setType(dto.getType());

        // If category is explicitly provided, use it; otherwise keep the existing one
        if (dto.getCategory() != null && !dto.getCategory().trim().isEmpty()) {
            transaction.setCategory(dto.getCategory());
        }

        return mapToDTO(transactionRepository.save(transaction));
    }

    public void deleteTransaction(Long id, String username) {
        Transaction transaction = transactionRepository
                .findById(Objects.requireNonNull(id, "Transaction id must not be null"))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Transaction not found"));

        // Ownership check — users can only delete their own transactions
        if (!transaction.getUser().getUsername().equals(username)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }

        transactionRepository.deleteById(Objects.requireNonNull(id));
    }

    private TransactionDTO mapToDTO(Transaction transaction) {
        return TransactionDTO.builder()
                .id(transaction.getId())
                .amount(transaction.getAmount())
                .description(transaction.getDescription())
                .date(transaction.getDate())
                .type(transaction.getType())
                .category(transaction.getCategory())
                .build();
    }
}
