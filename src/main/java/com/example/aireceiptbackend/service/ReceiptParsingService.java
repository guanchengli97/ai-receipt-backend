package com.example.aireceiptbackend.service;

import com.example.aireceiptbackend.model.*;
import com.example.aireceiptbackend.repository.ReceiptRepository;
import com.example.aireceiptbackend.repository.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.*;

@Service
public class ReceiptParsingService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final UserRepository userRepository;
    private final ReceiptRepository receiptRepository;
    private final String apiKey;
    private final String apiBase;
    private final String model;

    public ReceiptParsingService(
        RestTemplate restTemplate,
        ObjectMapper objectMapper,
        UserRepository userRepository,
        ReceiptRepository receiptRepository,
        @Value("${gemini.api-key:}") String apiKey,
        @Value("${gemini.api-base:https://generativelanguage.googleapis.com}") String apiBase,
        @Value("${gemini.model:gemini-2.5-flash-lite}") String model
    ) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.userRepository = userRepository;
        this.receiptRepository = receiptRepository;
        this.apiKey = apiKey;
        this.apiBase = apiBase;
        this.model = model;
    }

    public ReceiptParseResponse parseAndSaveFromUrl(String imageUrl, String email) throws IOException {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new IllegalStateException("Gemini API key is not configured");
        }
        if (imageUrl == null || imageUrl.trim().isEmpty()) {
            throw new IllegalArgumentException("Image URL is required");
        }
        URI uri = parseUri(imageUrl);
        validateHttpUrl(uri);

        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));

        ResponseEntity<byte[]> imageResponse;
        try {
            imageResponse = restTemplate.exchange(uri, HttpMethod.GET, new HttpEntity<>(new HttpHeaders()), byte[].class);
        } catch (RestClientException ex) {
            throw new IllegalArgumentException("Failed to download image");
        }

        if (!imageResponse.getStatusCode().is2xxSuccessful() || imageResponse.getBody() == null || imageResponse.getBody().length == 0) {
            throw new IllegalArgumentException("Image download returned empty data");
        }

        String mimeType = Optional.ofNullable(imageResponse.getHeaders().getContentType())
            .map(MediaType::toString)
            .orElse("image/jpeg");
        String base64 = Base64.getEncoder().encodeToString(imageResponse.getBody());

        String prompt = "Extract receipt data and return JSON only. " +
            "Use ISO-8601 date (YYYY-MM-DD). Include items with description, quantity, unitPrice, totalPrice.";

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
        receipt.setImageUrl(imageUrl);
        receipt.setRawJson(extraction != null ? safeToJson(extraction) : null);
        Receipt saved = receiptRepository.save(receipt);

        return toResponse(saved);
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
        receipt.setCurrency(trimToNull(extraction.getCurrency()));
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
        response.setImageUrl(receipt.getImageUrl());
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

    private URI parseUri(String value) {
        try {
            return new URI(value.trim());
        } catch (URISyntaxException ex) {
            throw new IllegalArgumentException("Invalid image URL");
        }
    }

    private void validateHttpUrl(URI uri) {
        if (uri == null || uri.getScheme() == null) {
            throw new IllegalArgumentException("Invalid image URL");
        }
        String scheme = uri.getScheme().toLowerCase(Locale.ROOT);
        if (!scheme.equals("http") && !scheme.equals("https")) {
            throw new IllegalArgumentException("Image URL must be http or https");
        }
    }
}
