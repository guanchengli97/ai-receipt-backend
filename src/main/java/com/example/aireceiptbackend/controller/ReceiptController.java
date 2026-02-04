package com.example.aireceiptbackend.controller;

import com.example.aireceiptbackend.model.ReceiptParseResponse;
import com.example.aireceiptbackend.model.ReceiptParseRequest;
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
        if (authentication == null || authentication.getName() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error("Unauthorized"));
        }

        try {
            List<ReceiptParseResponse> responses = receiptParsingService.getReceiptsByUserEmail(authentication.getName());
            return ResponseEntity.ok(responses);
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

    private Map<String, String> error(String message) {
        Map<String, String> payload = new HashMap<>();
        payload.put("error", message);
        return payload;
    }
}
