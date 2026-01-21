package com.example.aireceiptbackend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
public class HealthController {

    @GetMapping("/")
    public ResponseEntity<?> home() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "ok");
        response.put("message", "AI Receipt Backend is running");
        response.put("endpoints", new String[]{
                "POST /auth/login - login JWT token",
                "GET /api/health - health check"
        });
        return ResponseEntity.ok(response);
    }

    @GetMapping("/api/health")
    public ResponseEntity<?> health() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "healthy");
        response.put("service", "ai-receipt-backend");
        return ResponseEntity.ok(response);
    }
}
