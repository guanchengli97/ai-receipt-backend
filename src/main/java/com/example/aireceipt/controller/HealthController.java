package com.example.aireceipt.controller;

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
                "POST /api/auth/login - 登录获取 JWT token",
                "GET /api/health - 健康检查"
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
