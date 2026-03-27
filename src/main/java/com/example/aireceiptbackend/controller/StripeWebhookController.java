package com.example.aireceiptbackend.controller;

import com.example.aireceiptbackend.service.StripeBillingService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/billing")
public class StripeWebhookController {

    private final StripeBillingService stripeBillingService;

    public StripeWebhookController(StripeBillingService stripeBillingService) {
        this.stripeBillingService = stripeBillingService;
    }

    @PostMapping("/webhook")
    public ResponseEntity<?> handleWebhook(
        @RequestBody String payload,
        @RequestHeader(name = "Stripe-Signature", required = false) String signatureHeader
    ) {
        try {
            stripeBillingService.handleWebhookEvent(payload, signatureHeader);
            return ResponseEntity.ok(ok("received"));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(error(ex.getMessage()));
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(error(ex.getMessage()));
        }
    }

    private Map<String, String> ok(String message) {
        Map<String, String> payload = new HashMap<>();
        payload.put("status", message);
        return payload;
    }

    private Map<String, String> error(String message) {
        Map<String, String> payload = new HashMap<>();
        payload.put("error", message);
        return payload;
    }
}
