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
        // DB-level guarantee: no two rows can share the same name + user + type
        @UniqueConstraint(name = "uq_category_name_user_type", columnNames = {"name", "user_id", "type"})
    },
    indexes = {
        // Covers findByUser
        @Index(name = "idx_category_user_id", columnList = "user_id"),
        // Covers findByUserAndType — filters by both user and type
        @Index(name = "idx_category_user_type", columnList = "user_id, type"),
        // Covers existsByNameAndUserAndType — uniqueness check on every save
        @Index(name = "idx_category_name_user_type", columnList = "name, user_id, type")
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
