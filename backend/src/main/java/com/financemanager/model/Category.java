package com.financemanager.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
    name = "categories",
    uniqueConstraints = {
        // Enforces uniqueness and acts as composite B-tree index covering:
        // 1. findByUser (leftmost prefix: user_id)
        // 2. findByUserAndType (prefix: user_id, type)
        // 3. existsByNameAndUserAndType (full key: user_id, type, name)
        @UniqueConstraint(name = "uq_category_user_type_name", columnNames = {"user_id", "type", "name"})
    }
)
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType type; // INCOME or EXPENSE

    @Column(nullable = false)
    private String color; // Hex color code (e.g., #ef4444)

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
}
