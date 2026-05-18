package com.financemanager.controller;

import com.financemanager.dto.CategoryDTO;
import com.financemanager.model.TransactionType;
import com.financemanager.model.User;
import com.financemanager.repository.UserRepository;
import com.financemanager.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;
    private final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<List<CategoryDTO>> getAllCategories(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        return ResponseEntity.ok(categoryService.getAllCategories(user));
    }

    @GetMapping("/{type}")
    public ResponseEntity<List<CategoryDTO>> getCategoriesByType(
            @PathVariable TransactionType type,
            Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        return ResponseEntity.ok(categoryService.getCategoriesByType(user, type));
    }

    @PostMapping
    public ResponseEntity<CategoryDTO> createCategory(
            @RequestBody CategoryDTO categoryDTO,
            Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        return ResponseEntity.ok(categoryService.createCategory(categoryDTO, user));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CategoryDTO> updateCategory(
            @PathVariable Long id,
            @RequestBody CategoryDTO categoryDTO,
            Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        return ResponseEntity.ok(categoryService.updateCategory(id, categoryDTO, user));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCategory(
            @PathVariable Long id,
            Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        categoryService.deleteCategory(id, user);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/initialize")
    public ResponseEntity<Void> initializeDefaultCategories(Authentication authentication) {

        String username = authentication.getName(); // safer than casting
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        categoryService.initializeDefaultCategories(user);
        return ResponseEntity.ok().build();
    }
}
