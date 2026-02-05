package com.example.aireceiptbackend.controller;

import com.example.aireceiptbackend.model.ReceiptParseResponse;
import com.example.aireceiptbackend.model.ReceiptParseRequest;
import com.example.aireceiptbackend.model.ReceiptReviewRequest;
import com.example.aireceiptbackend.model.ReceiptStatsResponse;
import com.example.aireceiptbackend.service.ReceiptParsingService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/receipts")
public class ReceiptController {

    private final ReceiptParsingService receiptParsingService;

    public ReceiptController(ReceiptParsingService receiptParsingService) {
        this.receiptParsingService = receiptParsingService;
    }

    @GetMapping("/me")
    public ResponseEntity<?> getMyReceipts() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null || "anonymousUser".equals(authentication.getName())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error("Unauthorized"));
        }

        try {
            List<ReceiptParseResponse> responses = receiptParsingService.getReceiptsByUserEmail(authentication.getName());
            return ResponseEntity.ok(responses);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(error(ex.getMessage()));
        }
    }

    @GetMapping("/me/stats")
    public ResponseEntity<?> getMyReceiptStats() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null || "anonymousUser".equals(authentication.getName())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error("Unauthorized"));
        }

        try {
            ReceiptStatsResponse response = receiptParsingService.getMonthlyStats(authentication.getName());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(error(ex.getMessage()));
        }
    }

    @PostMapping(value = "/parse", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> parseReceipt(@RequestBody ReceiptParseRequest request) throws IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null || "anonymousUser".equals(authentication.getName())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error("Unauthorized"));
        }

        try {
            ReceiptParseResponse response = receiptParsingService.parseAndSaveFromImageId(
                request != null ? request.getImageId() : null,
                authentication.getName()
            );
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(error(ex.getMessage()));
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(error(ex.getMessage()));
        }
    }

    @PutMapping("/{id}/review")
    public ResponseEntity<?> updateReceiptReviewStatus(
        @PathVariable("id") Long id,
        @RequestBody ReceiptReviewRequest request
    ) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null || "anonymousUser".equals(authentication.getName())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error("Unauthorized"));
        }

        try {
            ReceiptParseResponse response = receiptParsingService.updateReviewStatus(
                id,
                request != null ? request.getReviewed() : null,
                authentication.getName()
            );
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(error(ex.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getReceiptById(@PathVariable("id") Long id) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null || "anonymousUser".equals(authentication.getName())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error("Unauthorized"));
        }

        try {
            ReceiptParseResponse response = receiptParsingService.getReceiptById(id, authentication.getName());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error(ex.getMessage()));
        }
    }

    private Map<String, String> error(String message) {
        Map<String, String> payload = new HashMap<>();
        payload.put("error", message);
        return payload;
    }
}
