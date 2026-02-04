package com.example.aireceiptbackend.service;

import com.example.aireceiptbackend.model.CreateUploadUrlRequest;
import com.example.aireceiptbackend.model.CreateUploadUrlResponse;
import com.example.aireceiptbackend.model.ImageAsset;
import com.example.aireceiptbackend.model.PresignedUrlResponse;
import com.example.aireceiptbackend.model.User;
import com.example.aireceiptbackend.repository.ImageAssetRepository;
import com.example.aireceiptbackend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Locale;
import java.util.UUID;

@Service
public class ImageStorageService {

    private final S3Presigner s3Presigner;
    private final UserRepository userRepository;
    private final ImageAssetRepository imageAssetRepository;
    private final String bucket;
    private final int uploadExpirySeconds;
    private final int downloadExpirySeconds;

    public ImageStorageService(
        S3Presigner s3Presigner,
        UserRepository userRepository,
        ImageAssetRepository imageAssetRepository,
        @Value("${aws.s3.bucket}") String bucket,
        @Value("${aws.s3.upload-expiry-seconds:300}") int uploadExpirySeconds,
        @Value("${aws.s3.download-expiry-seconds:120}") int downloadExpirySeconds
    ) {
        this.s3Presigner = s3Presigner;
        this.userRepository = userRepository;
        this.imageAssetRepository = imageAssetRepository;
        this.bucket = bucket;
        this.uploadExpirySeconds = uploadExpirySeconds;
        this.downloadExpirySeconds = downloadExpirySeconds;
    }

    public CreateUploadUrlResponse createUploadUrl(String principal, CreateUploadUrlRequest request) {
        User user = resolveUser(principal);
        String objectKey = buildObjectKey(user.getId(), request != null ? request.getFileName() : null);
        String contentType = normalizeContentType(request != null ? request.getContentType() : null);

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
            .bucket(bucket)
            .key(objectKey)
            .contentType(contentType)
            .build();

        Duration expiry = Duration.ofSeconds(uploadExpirySeconds);
        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
            .signatureDuration(expiry)
            .putObjectRequest(putObjectRequest)
            .build();

        PresignedPutObjectRequest presigned = s3Presigner.presignPutObject(presignRequest);
        Instant expiresAt = Instant.now().plusSeconds(uploadExpirySeconds);

        ImageAsset imageAsset = new ImageAsset();
        imageAsset.setUser(user);
        imageAsset.setObjectKey(objectKey);
        imageAsset.setOriginalFilename(trimToNull(request != null ? request.getFileName() : null));
        imageAsset.setContentType(contentType);
        imageAsset.setSizeBytes(request != null ? request.getSizeBytes() : null);
        imageAsset = imageAssetRepository.save(imageAsset);

        CreateUploadUrlResponse response = new CreateUploadUrlResponse();
        response.setImageId(imageAsset.getId());
        response.setObjectKey(objectKey);
        response.setUploadUrl(presigned.url().toString());
        response.setExpiresAt(expiresAt);
        return response;
    }

    public PresignedUrlResponse getPresignedDownloadUrl(String principal, Long imageId) {
        User user = resolveUser(principal);
        ImageAsset imageAsset = imageAssetRepository.findByIdAndUser(imageId, user)
            .orElseThrow(() -> new IllegalArgumentException("Image not found"));

        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
            .bucket(bucket)
            .key(imageAsset.getObjectKey())
            .build();

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
            .signatureDuration(Duration.ofSeconds(downloadExpirySeconds))
            .getObjectRequest(getObjectRequest)
            .build();
        PresignedGetObjectRequest presigned = s3Presigner.presignGetObject(presignRequest);

        PresignedUrlResponse response = new PresignedUrlResponse();
        response.setImageId(imageAsset.getId());
        response.setUrl(presigned.url().toString());
        response.setExpiresAt(Instant.now().plusSeconds(downloadExpirySeconds));
        return response;
    }

    private User resolveUser(String principal) {
        String normalized = trimToNull(principal);
        if (normalized == null) {
            throw new IllegalArgumentException("Unauthorized");
        }
        return userRepository.findByEmail(normalized)
            .or(() -> userRepository.findByUsername(normalized))
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    private String buildObjectKey(Long userId, String fileName) {
        LocalDate date = LocalDate.now();
        String ext = extractExtension(fileName);
        return String.format(
            "receipts/%d/%04d/%02d/%02d/%s%s",
            userId,
            date.getYear(),
            date.getMonthValue(),
            date.getDayOfMonth(),
            UUID.randomUUID(),
            ext
        );
    }

    private String extractExtension(String fileName) {
        String name = trimToNull(fileName);
        if (name == null) {
            return ".jpg";
        }
        int idx = name.lastIndexOf('.');
        if (idx < 0 || idx == name.length() - 1) {
            return ".jpg";
        }
        String ext = name.substring(idx).toLowerCase(Locale.ROOT);
        return ext.length() > 10 ? ".jpg" : ext;
    }

    private String normalizeContentType(String contentType) {
        String value = trimToNull(contentType);
        return value != null ? value : "image/jpeg";
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
