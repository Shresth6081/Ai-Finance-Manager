package com.financemanager.service;

import com.financemanager.dto.CategoryDTO;
import com.financemanager.model.Category;
import com.financemanager.model.TransactionType;
import com.financemanager.model.User;
import com.financemanager.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    @Transactional
    public CategoryDTO createCategory(CategoryDTO dto, User user) {
        // Check for duplicate category name for this user and type
        if (categoryRepository.existsByNameAndUserAndType(dto.getName(), user, dto.getType())) {
            throw new IllegalArgumentException(
                    "Category with name '" + dto.getName() + "' already exists for " + dto.getType());
        }

        Category category = Category.builder()
                .name(dto.getName())
                .type(dto.getType())
                .color(dto.getColor())
                .user(user)
                .build();

        Category saved = categoryRepository.save(category);
        return mapToDTO(saved);
    }

    public List<CategoryDTO> getAllCategories(User user) {
        return categoryRepository.findByUser(user).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public List<CategoryDTO> getCategoriesByType(User user, TransactionType type) {
        return categoryRepository.findByUserAndType(user, type).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public CategoryDTO updateCategory(Long id, CategoryDTO dto, User user) {
        Category category = categoryRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new IllegalArgumentException("Category not found"));

        // Check for duplicate name if name is being changed
        if (!category.getName().equals(dto.getName()) &&
                categoryRepository.existsByNameAndUserAndType(dto.getName(), user, dto.getType())) {
            throw new IllegalArgumentException("Category with name '" + dto.getName() + "' already exists");
        }

        category.setName(dto.getName());
        category.setType(dto.getType());
        category.setColor(dto.getColor());

        Category updated = categoryRepository.save(category);
        return mapToDTO(updated);
    }

    @Transactional
    public void deleteCategory(Long id, User user) {
        Category category = categoryRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new IllegalArgumentException("Category not found"));
        categoryRepository.delete(category);
    }

    @Transactional
    public void initializeDefaultCategories(User user) {
        // Check if user already has categories
        if (!categoryRepository.findByUser(user).isEmpty()) {
            return;
        }

        // Create default income categories
        createDefaultCategory("Salary", TransactionType.INCOME, "#22c55e", user);
        createDefaultCategory("Freelance", TransactionType.INCOME, "#10b981", user);
        createDefaultCategory("Investment", TransactionType.INCOME, "#059669", user);

        // Create default expense categories
        createDefaultCategory("Food", TransactionType.EXPENSE, "#ef4444", user);
        createDefaultCategory("Transport", TransactionType.EXPENSE, "#f97316", user);
        createDefaultCategory("Entertainment", TransactionType.EXPENSE, "#ec4899", user);
        createDefaultCategory("Utilities", TransactionType.EXPENSE, "#8b5cf6", user);
        createDefaultCategory("Shopping", TransactionType.EXPENSE, "#3b82f6", user);
    }

    private void createDefaultCategory(String name, TransactionType type, String color, User user) {
        Category category = Category.builder()
                .name(name)
                .type(type)
                .color(color)
                .user(user)
                .build();
        categoryRepository.save(category);
    }

    private CategoryDTO mapToDTO(Category category) {
        return CategoryDTO.builder()
                .id(category.getId())
                .name(category.getName())
                .type(category.getType())
                .color(category.getColor())
                .build();
    }
}
