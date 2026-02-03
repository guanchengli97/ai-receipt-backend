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

    public boolean register(String username, String email, String password) {
        if (username == null || username.trim().isEmpty() ||
            email == null || email.trim().isEmpty() ||
            password == null || password.trim().isEmpty()) {
            return false;
        }

        if (userRepository.existsByUsername(username) || userRepository.existsByEmail(email)) {
            return false;
        }

        User user = new User(username.trim(), email.trim(), passwordEncoder.encode(password));
        userRepository.save(user);
        return true;
    }
}
