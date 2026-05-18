package com.financemanager.repository;

import com.financemanager.model.Category;
import com.financemanager.model.TransactionType;
import com.financemanager.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    List<Category> findByUser(User user);

    List<Category> findByUserAndType(User user, TransactionType type);

    boolean existsByNameAndUserAndType(String name, User user, TransactionType type);

    Optional<Category> findByIdAndUser(Long id, User user);
}
