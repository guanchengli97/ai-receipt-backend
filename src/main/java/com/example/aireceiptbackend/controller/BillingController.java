package com.example.aireceiptbackend.controller;

import com.example.aireceiptbackend.model.*;
import com.example.aireceiptbackend.service.StripeBillingService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/billing")
public class BillingController {

    private final StripeBillingService stripeBillingService;

    public BillingController(StripeBillingService stripeBillingService) {
        this.stripeBillingService = stripeBillingService;
    }

    @PostMapping("/checkout-session")
    public ResponseEntity<?> createCheckoutSession(@RequestBody BillingCheckoutSessionRequest request) {
        String principal = currentPrincipal();
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error("Unauthorized"));
        }
        try {
            BillingSessionResponse response = stripeBillingService.createCheckoutSession(principal, request);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(error(ex.getMessage()));
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(error(ex.getMessage()));
        }
    }

    @PostMapping("/portal-session")
    public ResponseEntity<?> createPortalSession(@RequestBody BillingPortalSessionRequest request) {
        String principal = currentPrincipal();
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error("Unauthorized"));
        }
        try {
            BillingSessionResponse response = stripeBillingService.createPortalSession(principal, request);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(error(ex.getMessage()));
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(error(ex.getMessage()));
        }
    }

    @GetMapping("/me")
    public ResponseEntity<?> getMyBillingStatus() {
        String principal = currentPrincipal();
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error("Unauthorized"));
        }
        try {
            BillingStatusResponse response = stripeBillingService.getMyBillingStatus(principal);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(error(ex.getMessage()));
        }
    }

    @GetMapping("/me/usage")
    public ResponseEntity<?> getMyBillingUsage() {
        String principal = currentPrincipal();
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error("Unauthorized"));
        }
        try {
            BillingUsageResponse response = stripeBillingService.getMyBillingUsage(principal);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(error(ex.getMessage()));
        }
    }

    private String currentPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null || "anonymousUser".equals(authentication.getName())) {
            return null;
        }
        return authentication.getName();
    }

    private Map<String, String> error(String message) {
        Map<String, String> payload = new HashMap<>();
        payload.put("error", message);
        return payload;
    }
}
