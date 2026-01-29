package com.example.aireceiptbackend.controller;

import com.example.aireceiptbackend.model.AuthRequest;
import com.example.aireceiptbackend.model.AuthResponse;
import com.example.aireceiptbackend.model.RegisterResponse;
import com.example.aireceiptbackend.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody AuthRequest req) {
        String token = authService.login(req.getUsername(), req.getPassword());
        if (token == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(new AuthResponse(token));
    }

    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@RequestBody AuthRequest req) {
        // Validate input
        if (req.getUsername() == null || req.getUsername().trim().isEmpty() ||
            req.getPassword() == null || req.getPassword().trim().isEmpty()) {
            return ResponseEntity.badRequest()
                .body(new RegisterResponse(false, "Username and password are required"));
        }

        boolean success = authService.register(req.getUsername(), req.getPassword());
        if (success) {
            return ResponseEntity.ok(new RegisterResponse(true, "User registered successfully"));
        } else {
            return ResponseEntity.badRequest()
                .body(new RegisterResponse(false, "Username already exists or registration failed"));
        }
    }
}
