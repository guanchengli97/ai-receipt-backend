package com.example.aireceiptbackend.service;

import com.example.aireceiptbackend.model.User;
import com.example.aireceiptbackend.repository.UserRepository;
import com.example.aireceiptbackend.util.JwtUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AuthService {

    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;

    public AuthService(PasswordEncoder passwordEncoder, UserRepository userRepository) {
        this.passwordEncoder = passwordEncoder;
        this.userRepository = userRepository;
    }

    public String login(String username, String password) {
        if (username == null || username.trim().isEmpty() ||
            password == null || password.trim().isEmpty()) {
            return null;
        }

        Optional<User> userOpt = userRepository.findByUsername(username);
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
        return JwtUtil.generateToken(username);
    }

    public boolean register(String username, String password) {
        // Validate username and password
        if (username == null || username.trim().isEmpty() || 
            password == null || password.trim().isEmpty()) {
            return false; // Invalid username or password
        }

        // Check if user already exists
        if (userRepository.existsByUsername(username)) {
            return false; // User already exists
        }
        
        // Store the new user with encoded password
        String email = username + "@local";
        User user = new User(username, email, passwordEncoder.encode(password));
        userRepository.save(user);
        return true;
    }
}
