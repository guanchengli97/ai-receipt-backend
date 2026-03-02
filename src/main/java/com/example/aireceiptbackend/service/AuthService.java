package com.example.aireceiptbackend.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.example.aireceiptbackend.model.User;
import com.example.aireceiptbackend.repository.UserRepository;
import com.example.aireceiptbackend.util.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private static final int DEFAULT_USERNAME_MAX_LENGTH = 64;
    private static final Pattern NON_ALPHANUMERIC_PATTERN = Pattern.compile("[^a-zA-Z0-9_]");

    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final int activationTokenExpireHours;
    private final String googleClientId;
    private final GoogleIdTokenVerifier googleIdTokenVerifier;

    public AuthService(
        PasswordEncoder passwordEncoder,
        UserRepository userRepository,
        EmailService emailService,
        @Value("${app.auth.activation-token-expire-hours:24}") int activationTokenExpireHours,
        @Value("${app.auth.google.client-id:}") String googleClientId
    ) {
        this.passwordEncoder = passwordEncoder;
        this.userRepository = userRepository;
        this.emailService = emailService;
        this.activationTokenExpireHours = activationTokenExpireHours;
        this.googleClientId = googleClientId == null ? "" : googleClientId.trim();
        this.googleIdTokenVerifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), JacksonFactory.getDefaultInstance())
            .setAudience(Collections.singletonList(this.googleClientId))
            .build();
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

    @Transactional
    public String googleLogin(String idToken) {
        if (idToken == null || idToken.trim().isEmpty()) {
            return null;
        }
        if (googleClientId.isEmpty()) {
            log.warn("Google login attempted but app.auth.google.client-id is not configured");
            return null;
        }

        GoogleIdToken verifiedToken = verifyGoogleIdToken(idToken.trim());
        if (verifiedToken == null) {
            return null;
        }

        GoogleIdToken.Payload payload = verifiedToken.getPayload();
        String email = payload.getEmail();
        if (email == null || email.trim().isEmpty() || !Boolean.TRUE.equals(payload.getEmailVerified())) {
            return null;
        }

        String normalizedEmail = email.trim();
        String displayName = (String) payload.get("name");

        User user = userRepository.findByEmail(normalizedEmail).orElseGet(() -> {
            String username = buildUniqueUsername(displayName, normalizedEmail);
            String randomPassword = passwordEncoder.encode(UUID.randomUUID().toString());
            User newUser = new User(username, normalizedEmail, randomPassword);
            newUser.setIsActive(true);
            newUser.setEmailVerifiedAt(LocalDateTime.now());
            return userRepository.save(newUser);
        });

        if (Boolean.FALSE.equals(user.getIsActive())) {
            user.setIsActive(true);
            user.setEmailVerifiedAt(LocalDateTime.now());
            userRepository.save(user);
        }

        return JwtUtil.generateToken(user.getEmail());
    }

    private GoogleIdToken verifyGoogleIdToken(String idToken) {
        try {
            return googleIdTokenVerifier.verify(idToken);
        } catch (GeneralSecurityException | IOException e) {
            log.warn("Failed to verify Google ID token: {}", e.getMessage());
            return null;
        }
    }

    private String buildUniqueUsername(String displayName, String email) {
        String base = normalizeBaseUsername(displayName, email);
        String candidate = base;
        int suffix = 1;
        while (Boolean.TRUE.equals(userRepository.existsByUsername(candidate))) {
            candidate = appendSuffix(base, suffix++);
        }
        return candidate;
    }

    private String normalizeBaseUsername(String displayName, String email) {
        String source = displayName;
        if (source == null || source.trim().isEmpty()) {
            source = email.split("@")[0];
        }

        String sanitized = NON_ALPHANUMERIC_PATTERN.matcher(source.trim().replace(' ', '_')).replaceAll("");
        if (sanitized.isEmpty()) {
            sanitized = "google_user";
        }
        if (sanitized.length() > DEFAULT_USERNAME_MAX_LENGTH) {
            sanitized = sanitized.substring(0, DEFAULT_USERNAME_MAX_LENGTH);
        }
        return sanitized;
    }

    private String appendSuffix(String base, int suffix) {
        String suffixText = "_" + suffix;
        int maxBaseLength = DEFAULT_USERNAME_MAX_LENGTH - suffixText.length();
        if (maxBaseLength < 1) {
            maxBaseLength = 1;
        }

        String clippedBase = base.length() > maxBaseLength ? base.substring(0, maxBaseLength) : base;
        return clippedBase + suffixText;
    }
}
