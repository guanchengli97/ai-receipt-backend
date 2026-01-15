package com.example.aireceiptbackend.service;

import com.example.aireceiptbackend.util.JwtUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

@Service
public class AuthService {

    private final PasswordEncoder passwordEncoder;
    private final Map<String, String> users = new HashMap<>(); // username -> encoded password

    public AuthService(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    @PostConstruct
    public void init() {
        // Create a demo user: username=user, password=password
        users.put("user", passwordEncoder.encode("password"));
    }

    public String login(String username, String password) {
        if (!users.containsKey(username)) return null;
        String encoded = users.get(username);
        if (!passwordEncoder.matches(password, encoded)) return null;
        return JwtUtil.generateToken(username);
    }
}
