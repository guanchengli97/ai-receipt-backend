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
        String token = authService.login(req.getEmail(), req.getPassword());
        if (token == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(new AuthResponse(token));
    }

    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@RequestBody AuthRequest req) {
        String name = req.getName();
        if (name == null || name.trim().isEmpty()) {
            name = req.getUsername();
        }
        String email = req.getEmail();
        String password = req.getPassword();

        if (name == null || name.trim().isEmpty() ||
            email == null || email.trim().isEmpty() ||
            password == null || password.trim().isEmpty()) {
            return ResponseEntity.badRequest()
                .body(new RegisterResponse(false, "Name, email, and password are required"));
        }

        boolean success = authService.register(name, email, password);
        if (success) {
            return ResponseEntity.ok(new RegisterResponse(true, "User registered successfully"));
        } else {
            return ResponseEntity.badRequest()
                .body(new RegisterResponse(false, "Name or email already exists or registration failed"));
        }
    }
}
