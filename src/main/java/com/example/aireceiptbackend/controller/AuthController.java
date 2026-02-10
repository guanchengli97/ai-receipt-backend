package com.example.aireceiptbackend.controller;

import com.example.aireceiptbackend.model.AuthRequest;
import com.example.aireceiptbackend.model.AuthResponse;
import com.example.aireceiptbackend.model.RegisterResponse;
import com.example.aireceiptbackend.service.AuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);
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

        try {
            boolean success = authService.register(name, email, password);
            if (success) {
                return ResponseEntity.ok(new RegisterResponse(true, "Registered successfully. Please check your email to activate your account before login."));
            } else {
                return ResponseEntity.badRequest()
                    .body(new RegisterResponse(false, "Name or email already exists or registration failed"));
            }
        } catch (MailException e) {
            log.error("Failed to send activation email for register request: {}", e.getMessage(), e);
            return ResponseEntity.status(500)
                .body(new RegisterResponse(false, "Registration failed when sending activation email"));
        }
    }

    @GetMapping("/activate")
    public ResponseEntity<RegisterResponse> activateEmail(@RequestParam("token") String token) {
        boolean success = authService.activateEmail(token);
        if (!success) {
            return ResponseEntity.badRequest()
                .body(new RegisterResponse(false, "Activation link is invalid or expired"));
        }
        return ResponseEntity.ok(new RegisterResponse(true, "Email activated successfully. You can now login."));
    }
}
