package com.financemanager.repository;

import com.financemanager.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    // ORDER BY date DESC lets MySQL fully satisfy this with the
    // composite idx_transaction_user_date index (no extra file sort)
    @Query("SELECT t FROM Transaction t WHERE t.user.id = :userId ORDER BY t.date DESC")
    List<Transaction> findAllByUserId(@Param("userId") Long userId);
}
