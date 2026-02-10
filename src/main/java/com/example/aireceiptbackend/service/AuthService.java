package com.example.aireceiptbackend.service;

import com.example.aireceiptbackend.model.User;
import com.example.aireceiptbackend.repository.UserRepository;
import com.example.aireceiptbackend.util.JwtUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class AuthService {

    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final int activationTokenExpireHours;

    public AuthService(
        PasswordEncoder passwordEncoder,
        UserRepository userRepository,
        EmailService emailService,
        @Value("${app.auth.activation-token-expire-hours:24}") int activationTokenExpireHours
    ) {
        this.passwordEncoder = passwordEncoder;
        this.userRepository = userRepository;
        this.emailService = emailService;
        this.activationTokenExpireHours = activationTokenExpireHours;
    }

    public String login(String email, String password) {
        if (email == null || email.trim().isEmpty() ||
            password == null || password.trim().isEmpty()) {
            return null;
        }

        Optional<User> userOpt = userRepository.findByEmail(email.trim());
        if (userOpt.isEmpty()) {
            return null;
        }

        User user = userOpt.get();
        if (Boolean.FALSE.equals(user.getIsActive())) {
            return null;
        }

        if (!passwordEncoder.matches(password, user.getPassword())) {
            return null;
        }
        return JwtUtil.generateToken(user.getEmail());
    }

    @Transactional
    public boolean register(String username, String email, String password) {
        if (username == null || username.trim().isEmpty() ||
            email == null || email.trim().isEmpty() ||
            password == null || password.trim().isEmpty()) {
            return false;
        }

        String normalizedUsername = username.trim();
        String normalizedEmail = email.trim();

        if (userRepository.existsByUsername(normalizedUsername) || userRepository.existsByEmail(normalizedEmail)) {
            return false;
        }

        String token = UUID.randomUUID().toString().replace("-", "");
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(activationTokenExpireHours);

        User user = new User(normalizedUsername, normalizedEmail, passwordEncoder.encode(password));
        user.setIsActive(false);
        user.setEmailVerificationToken(token);
        user.setEmailVerificationExpiresAt(expiresAt);
        userRepository.save(user);

        emailService.sendActivationEmail(user.getEmail(), user.getUsername(), token);
        return true;
    }

    @Transactional
    public boolean activateEmail(String token) {
        if (token == null || token.trim().isEmpty()) {
            return false;
        }

        Optional<User> userOpt = userRepository.findByEmailVerificationToken(token.trim());
        if (userOpt.isEmpty()) {
            return false;
        }

        User user = userOpt.get();
        if (Boolean.TRUE.equals(user.getIsActive())) {
            return true;
        }

        if (user.getEmailVerificationExpiresAt() == null ||
            user.getEmailVerificationExpiresAt().isBefore(LocalDateTime.now())) {
            return false;
        }

        user.setIsActive(true);
        user.setEmailVerifiedAt(LocalDateTime.now());
        user.setEmailVerificationToken(null);
        user.setEmailVerificationExpiresAt(null);
        userRepository.save(user);
        return true;
    }
}
