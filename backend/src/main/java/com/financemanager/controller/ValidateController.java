package com.financemanager.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/validate")
public class ValidateController {

    @GetMapping
    public ResponseEntity<Void> validateToken() {
        return ResponseEntity.ok().build();
    }
}
