package com.example.aireceiptbackend.service;

import com.example.aireceiptbackend.model.*;
import com.example.aireceiptbackend.repository.ImageAssetRepository;
import com.example.aireceiptbackend.repository.ReceiptRepository;
import com.example.aireceiptbackend.repository.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.*;

@Service
public class ReceiptParsingService {

    private static final Set<String> ALLOWED_CATEGORIES = Set.of(
        "Housing",
        "Utilities",
        "Food",
        "Transportation",
        "Shopping",
        "Health",
        "Entertainment",
        "Subscriptions",
        "Travel",
        "Education"
    );

    private final RestTemplate restTemplate;
    private final S3Client s3Client;
    private final ObjectMapper objectMapper;
    private final UserRepository userRepository;
    private final ImageAssetRepository imageAssetRepository;
    private final ReceiptRepository receiptRepository;
    private final String apiKey;
    private final String apiBase;
    private final String model;
    private final String bucket;

    public ReceiptParsingService(
        RestTemplate restTemplate,
        S3Client s3Client,
        ObjectMapper objectMapper,
        UserRepository userRepository,
        ImageAssetRepository imageAssetRepository,
        ReceiptRepository receiptRepository,
        @Value("${gemini.api-key:}") String apiKey,
        @Value("${gemini.api-base:https://generativelanguage.googleapis.com}") String apiBase,
        @Value("${gemini.model:gemini-2.5-flash}") String model,
        @Value("${aws.s3.bucket}") String bucket
    ) {
        this.restTemplate = restTemplate;
        this.s3Client = s3Client;
        this.objectMapper = objectMapper;
        this.userRepository = userRepository;
        this.imageAssetRepository = imageAssetRepository;
        this.receiptRepository = receiptRepository;
        this.apiKey = apiKey;
        this.apiBase = apiBase;
        this.model = model;
        this.bucket = bucket;
    }

    public ReceiptParseResponse parseAndSaveFromImageId(Long imageId, String principal) throws IOException {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new IllegalStateException("Gemini API key is not configured");
        }
        if (imageId == null) {
            throw new IllegalArgumentException("imageId is required");
        }

        User user = resolveUser(principal);
        ImageAsset imageAsset = imageAssetRepository.findByIdAndUser(imageId, user)
            .orElseThrow(() -> new IllegalArgumentException("Image not found"));

        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
            .bucket(bucket)
            .key(imageAsset.getObjectKey())
            .build();

        byte[] imageBytes;
        try {
            ResponseBytes<?> objectBytes = s3Client.getObjectAsBytes(getObjectRequest);
            imageBytes = objectBytes.asByteArray();
        } catch (S3Exception ex) {
            throw new IllegalArgumentException("Failed to read image from storage");
        }
        if (imageBytes == null || imageBytes.length == 0) {
            throw new IllegalArgumentException("Stored image is empty");
        }

        String mimeType = trimToNull(imageAsset.getContentType());
        if (mimeType == null) {
            mimeType = "image/jpeg";
        }
        String base64 = Base64.getEncoder().encodeToString(imageBytes);

        String prompt = "Extract receipt data and return JSON only. " +
            "Use ISO-8601 date (YYYY-MM-DD). Include items with description, quantity, unitPrice, totalPrice, category(Housing, Utilities, Food, Transportation, Shopping, Health, Entertainment, Subscriptions, Travel, Education).";

        // String prompt = """
        //     Extract receipt data and return ONE valid JSON object only.
        //     Use ISO-8601 date (YYYY-MM-DD).
        //     Include items with subtotal, tax, total, and category if possible.
        //     JSON schema:
        //     {
        //     "receiptId": number | null,
        //     "merchantName": string,
        //     "receiptDate": string,
        //     "currency": string,
        //     "imageUrl": string | null,
        //     "subtotal": number | null,
        //     "tax": number | null,
        //     "total": number | null,
        //     "category": "Housing" | "Utilities" | "Food" | "Transportation" |
        //                 "Shopping" | "Health" | "Entertainment" |
        //                 "Subscriptions" | "Travel" | "Education" | "Other",
        //     "items": [
        //         {
        //         "description": string,
        //         "quantity": number | null,
        //         "unitPrice": number | null,
        //         "totalPrice": number | null
        //         }
        //     ]
        //     }
        //     """;




        Map<String, Object> requestBody = buildRequestBody(prompt, mimeType, base64);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-goog-api-key", apiKey);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
        String url = String.format("%s/v1beta/models/%s:generateContent", apiBase, model);
        ResponseEntity<String> response;
        try {
            response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
        } catch (RestClientException ex) {
            throw new IllegalStateException("Gemini API request failed");
        }

        ReceiptExtraction extraction = parseExtraction(response.getBody());
        Receipt receipt = mapToReceipt(extraction, user);
        receipt.setImageAsset(imageAsset);
        receipt.setImageUrl(buildStorageUrl(imageAsset.getObjectKey()));
        receipt.setIsReviewed(false);
        receipt.setRawJson(extraction != null ? safeToJson(extraction) : null);
        Receipt saved = receiptRepository.save(receipt);

        return toResponse(saved);
    }

    public List<ReceiptParseResponse> getReceiptsByUserEmail(String email) {
        User user = resolveUser(email);

        List<Receipt> receipts = receiptRepository.findByUser(user);
        receipts.sort(
            Comparator.comparing(
                Receipt::getCreatedAt,
                Comparator.nullsLast(Comparator.reverseOrder())
            ).thenComparing(
                Receipt::getId,
                Comparator.nullsLast(Comparator.reverseOrder())
            )
        );

        List<ReceiptParseResponse> responses = new ArrayList<>();
        for (Receipt receipt : receipts) {
            responses.add(toResponse(receipt));
        }
        return responses;
    }

    public ReceiptStatsResponse getMonthlyStats(String principal) {
        User user = resolveUser(principal);
        LocalDate monthStart = LocalDate.now().withDayOfMonth(1);
        LocalDate monthEnd = monthStart.plusMonths(1).minusDays(1);

        BigDecimal totalSpent = receiptRepository.sumTotalAmountByUserAndReceiptDateBetween(user, monthStart, monthEnd);
        long count = receiptRepository.countByUserAndReceiptDateGreaterThanEqualAndReceiptDateLessThanEqual(user, monthStart, monthEnd);

        ReceiptStatsResponse response = new ReceiptStatsResponse();
        response.setTotalSpentThisMonth(totalSpent != null ? totalSpent : BigDecimal.ZERO);
        response.setReceiptsProcessedThisMonth(count);
        return response;
    }

    public CategorySpendingStatsResponse getMonthlySpendingByCategory(String principal) {
        User user = resolveUser(principal);
        LocalDate monthStart = LocalDate.now().withDayOfMonth(1);
        LocalDate monthEnd = monthStart.plusMonths(1).minusDays(1);

        List<Receipt> receipts = receiptRepository
            .findByUserAndReceiptDateGreaterThanEqualAndReceiptDateLessThanEqualOrderByReceiptDateDesc(
                user,
                monthStart,
                monthEnd
            );

        Map<String, BigDecimal> categoryTotals = new HashMap<>();
        for (Receipt receipt : receipts) {
            String category = normalizeCategoryForStats(receipt.getCategory());
            BigDecimal amount = receipt.getTotalAmount() != null ? receipt.getTotalAmount() : BigDecimal.ZERO;
            categoryTotals.merge(category, amount, BigDecimal::add);
        }

        List<CategorySpendingStatsResponse.CategorySpending> categories = new ArrayList<>();
        for (Map.Entry<String, BigDecimal> entry : categoryTotals.entrySet()) {
            CategorySpendingStatsResponse.CategorySpending spending = new CategorySpendingStatsResponse.CategorySpending();
            spending.setCategory(entry.getKey());
            spending.setAmount(entry.getValue() != null ? entry.getValue() : BigDecimal.ZERO);
            categories.add(spending);
        }
        categories.sort(
            Comparator.comparing(CategorySpendingStatsResponse.CategorySpending::getAmount, Comparator.reverseOrder())
                .thenComparing(CategorySpendingStatsResponse.CategorySpending::getCategory)
        );

        BigDecimal totalSpent = BigDecimal.ZERO;
        for (CategorySpendingStatsResponse.CategorySpending spending : categories) {
            totalSpent = totalSpent.add(spending.getAmount() != null ? spending.getAmount() : BigDecimal.ZERO);
        }

        CategorySpendingStatsResponse response = new CategorySpendingStatsResponse();
        response.setMonthStart(monthStart);
        response.setMonthEnd(monthEnd);
        response.setCurrency("USD");
        response.setTotalSpent(totalSpent);
        response.setCategories(categories);
        return response;
    }

    public ReceiptParseResponse updateReviewStatus(Long receiptId, Boolean reviewed, String principal) {
        if (receiptId == null) {
            throw new IllegalArgumentException("receiptId is required");
        }
        if (reviewed == null) {
            throw new IllegalArgumentException("reviewed is required");
        }

        User user = resolveUser(principal);
        Receipt receipt = receiptRepository.findByIdAndUser(receiptId, user)
            .orElseThrow(() -> new IllegalArgumentException("Receipt not found"));

        receipt.setIsReviewed(reviewed);
        Receipt saved = receiptRepository.save(receipt);
        return toResponse(saved);
    }

    public ReceiptParseResponse getReceiptById(Long receiptId, String principal) {
        if (receiptId == null) {
            throw new IllegalArgumentException("receiptId is required");
        }
        User user = resolveUser(principal);
        Receipt receipt = receiptRepository.findByIdAndUser(receiptId, user)
            .orElseThrow(() -> new IllegalArgumentException("Receipt not found"));
        return toResponse(receipt);
    }

    public ReceiptParseResponse updateReceiptDetails(Long receiptId, ReceiptUpdateRequest request, String principal) {
        if (receiptId == null) {
            throw new IllegalArgumentException("receiptId is required");
        }
        if (request == null) {
            throw new IllegalArgumentException("request is required");
        }
        User user = resolveUser(principal);
        Receipt receipt = receiptRepository.findByIdAndUser(receiptId, user)
            .orElseThrow(() -> new IllegalArgumentException("Receipt not found"));

        if (request.getMerchantName() != null) {
            receipt.setMerchantName(trimToNull(request.getMerchantName()));
        }
        if (request.getReceiptDate() != null) {
            receipt.setReceiptDate(request.getReceiptDate());
        }
        if (request.getCurrency() != null) {
            String currency = trimToNull(request.getCurrency());
            receipt.setCurrency(currency != null ? currency.toUpperCase() : null);
        }
        if (request.getCategory() != null) {
            receipt.setCategory(trimToNull(request.getCategory()));
        }
        if (request.getSubtotal() != null) {
            receipt.setSubtotalAmount(request.getSubtotal());
        }
        if (request.getTax() != null) {
            receipt.setTaxAmount(request.getTax());
        }
        if (request.getTotal() != null) {
            receipt.setTotalAmount(request.getTotal());
        }

        if (request.getItems() != null) {
            receipt.getItems().clear();
            for (ReceiptUpdateRequest.ReceiptUpdateItem itemRequest : request.getItems()) {
                if (itemRequest == null) {
                    continue;
                }
                ReceiptItem item = new ReceiptItem();
                item.setDescription(trimToNull(itemRequest.getDescription()));
                item.setQuantity(itemRequest.getQuantity());
                item.setUnitPrice(itemRequest.getUnitPrice());
                item.setTotalPrice(itemRequest.getTotalPrice());
                receipt.addItem(item);
            }
        }

        Receipt saved = receiptRepository.save(receipt);
        return toResponse(saved);
    }

    @Transactional
    public ReceiptDeleteResponse deleteReceipt(Long receiptId, String principal) {
        if (receiptId == null) {
            throw new IllegalArgumentException("receiptId is required");
        }

        User user = resolveUser(principal);
        Receipt receipt = receiptRepository.findByIdAndUser(receiptId, user)
            .orElseThrow(() -> new IllegalArgumentException("Receipt not found"));

        ImageAsset imageAsset = receipt.getImageAsset();
        receiptRepository.delete(receipt);
        cleanupOrphanImageAssets(Collections.singletonList(imageAsset));
        return new ReceiptDeleteResponse(1, Collections.singletonList(receiptId));
    }

    @Transactional
    public ReceiptDeleteResponse deleteReceipts(List<Long> receiptIds, String principal) {
        if (receiptIds == null || receiptIds.isEmpty()) {
            throw new IllegalArgumentException("ids is required");
        }

        LinkedHashSet<Long> uniqueIds = new LinkedHashSet<>();
        for (Long id : receiptIds) {
            if (id == null) {
                throw new IllegalArgumentException("ids must not contain null");
            }
            uniqueIds.add(id);
        }

        User user = resolveUser(principal);
        List<Receipt> receipts = receiptRepository.findByIdInAndUser(uniqueIds, user);

        Set<Long> foundIds = new HashSet<>();
        for (Receipt receipt : receipts) {
            foundIds.add(receipt.getId());
        }

        List<Long> missingIds = new ArrayList<>();
        for (Long id : uniqueIds) {
            if (!foundIds.contains(id)) {
                missingIds.add(id);
            }
        }
        if (!missingIds.isEmpty()) {
            throw new IllegalArgumentException("Receipts not found: " + missingIds);
        }

        List<ImageAsset> imageAssets = new ArrayList<>();
        for (Receipt receipt : receipts) {
            imageAssets.add(receipt.getImageAsset());
        }

        receiptRepository.deleteAll(receipts);
        cleanupOrphanImageAssets(imageAssets);
        List<Long> deletedIds = new ArrayList<>(uniqueIds);
        return new ReceiptDeleteResponse(deletedIds.size(), deletedIds);
    }

    private void cleanupOrphanImageAssets(List<ImageAsset> imageAssets) {
        if (imageAssets == null || imageAssets.isEmpty()) {
            return;
        }

        LinkedHashMap<Long, ImageAsset> distinctAssets = new LinkedHashMap<>();
        for (ImageAsset imageAsset : imageAssets) {
            if (imageAsset == null || imageAsset.getId() == null) {
                continue;
            }
            distinctAssets.putIfAbsent(imageAsset.getId(), imageAsset);
        }

        for (ImageAsset imageAsset : distinctAssets.values()) {
            if (receiptRepository.existsByImageAsset(imageAsset)) {
                continue;
            }

            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                .bucket(bucket)
                .key(imageAsset.getObjectKey())
                .build();
            try {
                s3Client.deleteObject(deleteObjectRequest);
            } catch (S3Exception ex) {
                throw new IllegalStateException("Failed to delete image from storage");
            }

            imageAssetRepository.delete(imageAsset);
        }
    }

    public List<ReceiptParseResponse> getReceiptsByDateRange(LocalDate startDate, LocalDate endDate, String principal) {
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("start and end are required");
        }
        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("end must be on or after start");
        }

        User user = resolveUser(principal);
        List<Receipt> receipts = receiptRepository
            .findByUserAndReceiptDateGreaterThanEqualAndReceiptDateLessThanEqualOrderByReceiptDateDesc(
                user,
                startDate,
                endDate
            );

        List<ReceiptParseResponse> responses = new ArrayList<>();
        for (Receipt receipt : receipts) {
            responses.add(toResponse(receipt));
        }
        return responses;
    }

    private Map<String, Object> buildRequestBody(String prompt, String mimeType, String base64) {
        Map<String, Object> inlineData = new HashMap<>();
        inlineData.put("mime_type", mimeType);
        inlineData.put("data", base64);

        Map<String, Object> imagePart = new HashMap<>();
        imagePart.put("inline_data", inlineData);

        Map<String, Object> textPart = new HashMap<>();
        textPart.put("text", prompt);

        Map<String, Object> content = new HashMap<>();
        content.put("parts", Arrays.asList(textPart, imagePart));

        Map<String, Object> schema = new HashMap<>();
        schema.put("type", "object");
        Map<String, Object> properties = new HashMap<>();
        properties.put("merchantName", mapSchemaType("string"));
        properties.put("receiptDate", mapSchemaType("string"));
        properties.put("currency", mapSchemaType("string"));
        properties.put("category", mapSchemaType("string"));
        properties.put("subtotal", mapSchemaType("string"));
        properties.put("tax", mapSchemaType("string"));
        properties.put("total", mapSchemaType("string"));
        Map<String, Object> itemSchema = new HashMap<>();
        itemSchema.put("type", "object");
        Map<String, Object> itemProps = new HashMap<>();
        itemProps.put("description", mapSchemaType("string"));
        itemProps.put("quantity", mapSchemaType("string"));
        itemProps.put("unitPrice", mapSchemaType("string"));
        itemProps.put("totalPrice", mapSchemaType("string"));
        itemSchema.put("properties", itemProps);
        Map<String, Object> itemsArray = new HashMap<>();
        itemsArray.put("type", "array");
        itemsArray.put("items", itemSchema);
        properties.put("items", itemsArray);
        schema.put("properties", properties);

        Map<String, Object> generationConfig = new HashMap<>();
        generationConfig.put("responseMimeType", "application/json");
        generationConfig.put("responseSchema", schema);

        Map<String, Object> body = new HashMap<>();
        body.put("contents", Collections.singletonList(content));
        body.put("generationConfig", generationConfig);
        return body;
    }

    private Map<String, Object> mapSchemaType(String type) {
        Map<String, Object> schema = new HashMap<>();
        schema.put("type", type);
        return schema;
    }

    private ReceiptExtraction parseExtraction(String responseBody) throws JsonProcessingException {
        if (responseBody == null || responseBody.trim().isEmpty()) {
            throw new IllegalStateException("Empty response from Gemini");
        }

        JsonNode root = objectMapper.readTree(responseBody);
        JsonNode textNode = root.path("candidates").path(0).path("content").path("parts").path(0).path("text");
        if (textNode.isMissingNode()) {
            throw new IllegalStateException("No text response from Gemini");
        }

        String jsonText = extractJson(textNode.asText());
        return objectMapper.readValue(jsonText, ReceiptExtraction.class);
    }

    private String extractJson(String text) {
        if (text == null) {
            return null;
        }
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return text.substring(start, end + 1);
        }
        return text.trim();
    }

    private Receipt mapToReceipt(ReceiptExtraction extraction, User user) {
        Receipt receipt = new Receipt();
        receipt.setUser(user);
        if (extraction == null) {
            return receipt;
        }
        receipt.setMerchantName(trimToNull(extraction.getMerchantName()));
        receipt.setCurrency(normalizeCurrency(extraction.getCurrency()));
        receipt.setCategory(normalizeCategory(extraction.getCategory()));
        receipt.setReceiptDate(parseDate(extraction.getReceiptDate()));
        receipt.setSubtotalAmount(parseAmount(extraction.getSubtotal()));
        receipt.setTaxAmount(parseAmount(extraction.getTax()));
        receipt.setTotalAmount(parseAmount(extraction.getTotal()));

        if (extraction.getItems() != null) {
            for (ReceiptItemExtraction itemExtraction : extraction.getItems()) {
                ReceiptItem item = new ReceiptItem();
                item.setDescription(trimToNull(itemExtraction.getDescription()));
                item.setQuantity(parseAmount(itemExtraction.getQuantity()));
                item.setUnitPrice(parseAmount(itemExtraction.getUnitPrice()));
                item.setTotalPrice(parseAmount(itemExtraction.getTotalPrice()));
                receipt.addItem(item);
            }
        }
        return receipt;
    }

    private ReceiptParseResponse toResponse(Receipt receipt) {
        ReceiptParseResponse response = new ReceiptParseResponse();
        response.setReceiptId(receipt.getId());
        response.setMerchantName(receipt.getMerchantName());
        response.setReceiptDate(receipt.getReceiptDate());
        response.setCurrency(receipt.getCurrency());
        response.setCategory(receipt.getCategory());
        response.setImageId(receipt.getImageAsset() != null ? receipt.getImageAsset().getId() : null);
        response.setImageUrl(receipt.getImageUrl());
        response.setReviewed(Boolean.TRUE.equals(receipt.getIsReviewed()));
        response.setSubtotal(receipt.getSubtotalAmount());
        response.setTax(receipt.getTaxAmount());
        response.setTotal(receipt.getTotalAmount());
        if (receipt.getItems() != null) {
            List<ReceiptParseResponse.ReceiptParseItem> items = new ArrayList<>();
            for (ReceiptItem item : receipt.getItems()) {
                ReceiptParseResponse.ReceiptParseItem respItem = new ReceiptParseResponse.ReceiptParseItem();
                respItem.setDescription(item.getDescription());
                respItem.setQuantity(item.getQuantity());
                respItem.setUnitPrice(item.getUnitPrice());
                respItem.setTotalPrice(item.getTotalPrice());
                items.add(respItem);
            }
            response.setItems(items);
        }
        return response;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String normalizeCurrency(String currency) {
        String normalized = trimToNull(currency);
        return normalized != null ? normalized.toUpperCase() : "USD";
    }

    private String normalizeCategory(String category) {
        String normalized = trimToNull(category);
        if (normalized == null) {
            return "Other";
        }
        for (String allowedCategory : ALLOWED_CATEGORIES) {
            if (allowedCategory.equalsIgnoreCase(normalized)) {
                return allowedCategory;
            }
        }
        return "Other";
    }

    private String normalizeCategoryForStats(String category) {
        String normalized = trimToNull(category);
        return normalized != null ? normalized : "Other";
    }

    private LocalDate parseDate(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(value.trim());
        } catch (DateTimeParseException ignored) {
            try {
                LocalDateTime dateTime = LocalDateTime.parse(value.trim());
                return dateTime.toLocalDate();
            } catch (DateTimeParseException ignoredAgain) {
                return null;
            }
        }
    }

    private BigDecimal parseAmount(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        String cleaned = value.replaceAll("[^0-9.\\-]", "");
        if (cleaned.isEmpty() || "-".equals(cleaned) || ".".equals(cleaned)) {
            return null;
        }
        try {
            return new BigDecimal(cleaned);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String safeToJson(ReceiptExtraction extraction) {
        try {
            return objectMapper.writeValueAsString(extraction);
        } catch (JsonProcessingException ex) {
            return null;
        }
    }

    private User resolveUser(String principal) {
        String normalized = trimToNull(principal);
        if (normalized == null || "anonymousUser".equals(normalized)) {
            throw new IllegalArgumentException("Unauthorized");
        }
        return userRepository.findByEmail(normalized)
            .or(() -> userRepository.findByUsername(normalized))
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    private String buildStorageUrl(String objectKey) {
        return "s3://" + bucket + "/" + objectKey;
    }
}
