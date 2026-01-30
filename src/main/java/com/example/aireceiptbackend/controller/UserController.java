package com.example.aireceiptbackend.controller;

import com.example.aireceiptbackend.model.User;
import com.example.aireceiptbackend.model.UserProfileResponse;
import com.example.aireceiptbackend.model.UserUpdateRequest;
import com.example.aireceiptbackend.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserController(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/{username}")
    public ResponseEntity<UserProfileResponse> getProfile(@PathVariable String username) {
        if (username == null || username.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        if (!isSelf(username)) {
            return ResponseEntity.status(403).build();
        }

        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(toProfile(userOpt.get()));
    }

    @PutMapping("/{username}")
    public ResponseEntity<UserProfileResponse> updateProfile(
        @PathVariable String username,
        @RequestBody UserUpdateRequest req
    ) {
        if (username == null || username.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        if (!isSelf(username)) {
            return ResponseEntity.status(403).build();
        }

        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        if (req == null) {
            return ResponseEntity.badRequest().build();
        }

        User user = userOpt.get();
        boolean changed = false;

        if (req.getEmail() != null && !req.getEmail().trim().isEmpty()) {
            String newEmail = req.getEmail().trim();
            if (!newEmail.equalsIgnoreCase(user.getEmail())) {
                if (Boolean.TRUE.equals(userRepository.existsByEmail(newEmail))) {
                    return ResponseEntity.badRequest().build();
                }
                user.setEmail(newEmail);
                changed = true;
            }
        }

        if (req.getPassword() != null && !req.getPassword().trim().isEmpty()) {
            user.setPassword(passwordEncoder.encode(req.getPassword()));
            changed = true;
        }

        if (!changed) {
            return ResponseEntity.badRequest().build();
        }

        User saved = userRepository.save(user);
        return ResponseEntity.ok(toProfile(saved));
    }

    private UserProfileResponse toProfile(User user) {
        return new UserProfileResponse(
            user.getId(),
            user.getUsername(),
            user.getEmail(),
            user.getIsActive(),
            user.getCreatedAt(),
            user.getUpdatedAt()
        );
    }

    private boolean isSelf(String username) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return false;
        }
        String authName = auth.getName();
        return authName != null && authName.equals(username);
    }
}
