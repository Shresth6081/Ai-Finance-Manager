package com.financemanager.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Token validation endpoint — lives under /api/v1/validate which is NOT in the
 * Security permitAll list, so Spring Security's JWT filter will reject
 * missing/expired/invalid tokens with a 401 before this method is ever reached.
 * The frontend calls GET /api/v1/validate on startup to confirm the stored
 * token is still accepted by the backend.
 */
@RestController
@RequestMapping("/api/v1/validate")
public class ValidateController {

    @GetMapping
    public ResponseEntity<Void> validateToken() {
        return ResponseEntity.ok().build();
    }
}
