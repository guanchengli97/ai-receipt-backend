package com.example.aireceiptbackend.controller;

import com.example.aireceiptbackend.model.CreateUploadUrlRequest;
import com.example.aireceiptbackend.model.CreateUploadUrlResponse;
import com.example.aireceiptbackend.model.PresignedUrlResponse;
import com.example.aireceiptbackend.service.ImageStorageService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/images")
public class ImageController {

    private final ImageStorageService imageStorageService;

    public ImageController(ImageStorageService imageStorageService) {
        this.imageStorageService = imageStorageService;
    }

    @PostMapping("/upload-url")
    public ResponseEntity<?> createUploadUrl(@RequestBody(required = false) CreateUploadUrlRequest request) {
        String principal = currentPrincipal();
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error("Unauthorized"));
        }

        try {
            CreateUploadUrlResponse response = imageStorageService.createUploadUrl(principal, request);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(error(ex.getMessage()));
        }
    }

    @GetMapping("/{id}/presigned-url")
    public ResponseEntity<?> getPresignedUrl(@PathVariable("id") Long id) {
        String principal = currentPrincipal();
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error("Unauthorized"));
        }

        try {
            PresignedUrlResponse response = imageStorageService.getPresignedDownloadUrl(principal, id);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error(ex.getMessage()));
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
