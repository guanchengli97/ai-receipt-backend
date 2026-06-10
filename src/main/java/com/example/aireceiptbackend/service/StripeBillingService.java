package com.example.aireceiptbackend.service;

import com.example.aireceiptbackend.model.*;
import com.example.aireceiptbackend.repository.ReceiptRepository;
import com.example.aireceiptbackend.repository.StripeWebhookEventRepository;
import com.example.aireceiptbackend.repository.UserRepository;
import com.stripe.Stripe;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.Invoice;
import com.stripe.model.StripeObject;
import com.stripe.model.Subscription;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.checkout.SessionCreateParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

@Service
public class StripeBillingService {

    private static final Logger log = LoggerFactory.getLogger(StripeBillingService.class);

    private static final String PLAN_FREE = "FREE";
    private static final String PLAN_PRO = "PRO";

    private final UserRepository userRepository;
    private final ReceiptRepository receiptRepository;
    private final StripeWebhookEventRepository stripeWebhookEventRepository;
    private final String stripeSecretKey;
    private final String stripeWebhookSecret;
    private final int dailyLimitFree;
    private final int dailyLimitPro;
    private final Set<String> allowedPriceIds = new HashSet<>();

    public StripeBillingService(
        UserRepository userRepository,
        ReceiptRepository receiptRepository,
        StripeWebhookEventRepository stripeWebhookEventRepository,
        @Value("${stripe.secret-key:}") String stripeSecretKey,
        @Value("${stripe.webhook-secret:}") String stripeWebhookSecret,
        @Value("${stripe.price.pro-monthly:}") String proMonthlyPriceId,
        @Value("${stripe.price.pro-yearly:}") String proYearlyPriceId,
        @Value("${app.receipt.daily-limit-free:${app.receipt.daily-limit-normal:3}}") int dailyLimitFree,
        @Value("${app.receipt.daily-limit-pro:100}") int dailyLimitPro
    ) {
        this.userRepository = userRepository;
        this.receiptRepository = receiptRepository;
        this.stripeWebhookEventRepository = stripeWebhookEventRepository;
        this.stripeSecretKey = stripeSecretKey == null ? "" : stripeSecretKey.trim();
        this.stripeWebhookSecret = stripeWebhookSecret == null ? "" : stripeWebhookSecret.trim();
        this.dailyLimitFree = dailyLimitFree;
        this.dailyLimitPro = dailyLimitPro;
        addPriceId(proMonthlyPriceId);
        addPriceId(proYearlyPriceId);
    }

    @Transactional
    public BillingSessionResponse createCheckoutSession(String principal, BillingCheckoutSessionRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request is required");
        }
        String priceId = trimToNull(request.getPriceId());
        String successUrl = trimToNull(request.getSuccessUrl());
        String cancelUrl = trimToNull(request.getCancelUrl());
        if (priceId == null || successUrl == null || cancelUrl == null) {
            throw new IllegalArgumentException("priceId, successUrl, and cancelUrl are required");
        }
        if (!allowedPriceIds.contains(priceId)) {
            throw new IllegalArgumentException("Unsupported priceId");
        }

        ensureStripeApiConfigured();
        Stripe.apiKey = stripeSecretKey;

        User user = resolveUser(principal);
        String stripeCustomerId = ensureStripeCustomerId(user);

        try {
            SessionCreateParams.Builder builder = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
                .setCustomer(stripeCustomerId)
                .setSuccessUrl(successUrl)
                .setCancelUrl(cancelUrl)
                .putMetadata("userId", String.valueOf(user.getId()))
                .putMetadata("userEmail", user.getEmail())
                .addLineItem(
                    SessionCreateParams.LineItem.builder()
                        .setPrice(priceId)
                        .setQuantity(1L)
                        .build()
                );
            Session session = Session.create(builder.build());
            if (session.getUrl() == null || session.getUrl().trim().isEmpty()) {
                throw new IllegalStateException("Stripe checkout URL is empty");
            }
            return new BillingSessionResponse(session.getUrl());
        } catch (StripeException ex) {
            throw new IllegalStateException("Failed to create Stripe checkout session");
        }
    }

    @Transactional
    public BillingSessionResponse createPortalSession(String principal, BillingPortalSessionRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request is required");
        }
        String returnUrl = trimToNull(request.getReturnUrl());
        if (returnUrl == null) {
            throw new IllegalArgumentException("returnUrl is required");
        }

        ensureStripeApiConfigured();
        Stripe.apiKey = stripeSecretKey;

        User user = resolveUser(principal);
        String stripeCustomerId = trimToNull(user.getStripeCustomerId());
        if (stripeCustomerId == null) {
            throw new IllegalArgumentException("Stripe customer not found for current user");
        }

        try {
            com.stripe.param.billingportal.SessionCreateParams params = com.stripe.param.billingportal.SessionCreateParams.builder()
                .setCustomer(stripeCustomerId)
                .setReturnUrl(returnUrl)
                .build();
            com.stripe.model.billingportal.Session session = com.stripe.model.billingportal.Session.create(params);
            if (session.getUrl() == null || session.getUrl().trim().isEmpty()) {
                throw new IllegalStateException("Stripe portal URL is empty");
            }
            return new BillingSessionResponse(session.getUrl());
        } catch (StripeException ex) {
            throw new IllegalStateException("Failed to create Stripe portal session");
        }
    }

    @Transactional(readOnly = true)
    public BillingStatusResponse getMyBillingStatus(String principal) {
        User user = resolveUser(principal);
        BillingStatusResponse response = new BillingStatusResponse();
        response.setPlan(normalizePlan(user.getPlan()));
        response.setSubscriptionStatus(trimToNull(user.getSubscriptionStatus()));
        response.setSubscriptionCurrentPeriodEnd(user.getSubscriptionCurrentPeriodEnd());
        response.setDailyLimit(resolveDailyLimit(user));
        return response;
    }

    @Transactional(readOnly = true)
    public BillingUsageResponse getMyBillingUsage(String principal) {
        User user = resolveUser(principal);
        int dailyLimit = resolveDailyLimit(user);
        long usedToday = countUsedToday(user);

        BillingUsageResponse response = new BillingUsageResponse();
        response.setPlan(normalizePlan(user.getPlan()));
        response.setSubscriptionStatus(trimToNull(user.getSubscriptionStatus()));
        response.setSubscriptionCurrentPeriodEnd(user.getSubscriptionCurrentPeriodEnd());
        response.setDailyLimit(dailyLimit);
        response.setUsedToday(usedToday);
        response.setRemainingToday(calculateRemainingToday(dailyLimit, usedToday));
        return response;
    }

    @Transactional
    public void handleWebhookEvent(String payload, String signatureHeader) {
        ensureStripeWebhookConfigured();
        if (payload == null || payload.trim().isEmpty()) {
            throw new IllegalArgumentException("Webhook payload is empty");
        }
        if (signatureHeader == null || signatureHeader.trim().isEmpty()) {
            throw new IllegalArgumentException("Missing Stripe signature");
        }

        Event event;
        try {
            event = Webhook.constructEvent(payload, signatureHeader, stripeWebhookSecret);
        } catch (SignatureVerificationException ex) {
            throw new IllegalArgumentException("Invalid Stripe signature");
        }

        String eventId = trimToNull(event.getId());
        if (eventId == null) {
            throw new IllegalArgumentException("Stripe event id is missing");
        }
        if (stripeWebhookEventRepository.existsByEventId(eventId)) {
            return;
        }

        StripeObject stripeObject = deserializeStripeObject(event);
        if (stripeObject != null) {
            processStripeEvent(event.getType(), stripeObject);
        } else {
            log.warn("Skipping Stripe webhook event {} ({}) because data.object could not be deserialized", eventId, event.getType());
        }

        stripeWebhookEventRepository.save(
            new StripeWebhookEvent(eventId, trimToNull(event.getType()) != null ? event.getType() : "unknown", LocalDateTime.now())
        );
    }

    private StripeObject deserializeStripeObject(Event event) {
        Optional<StripeObject> stripeObject = event.getDataObjectDeserializer().getObject();
        if (stripeObject.isPresent()) {
            return stripeObject.get();
        }

        try {
            return event.getDataObjectDeserializer().deserializeUnsafe();
        } catch (Exception ex) {
            log.warn("Failed to deserialize Stripe webhook event {} ({})", event.getId(), event.getType(), ex);
            return null;
        }
    }

    private void processStripeEvent(String eventType, StripeObject stripeObject) {
        if (eventType == null) {
            return;
        }
        switch (eventType) {
            case "checkout.session.completed":
                if (stripeObject instanceof Session) {
                    handleCheckoutCompleted((Session) stripeObject);
                }
                break;
            case "customer.subscription.created":
            case "customer.subscription.updated":
            case "customer.subscription.deleted":
                if (stripeObject instanceof Subscription) {
                    syncUserFromSubscription((Subscription) stripeObject);
                }
                break;
            case "invoice.paid":
            case "invoice.payment_failed":
                if (stripeObject instanceof Invoice) {
                    handleInvoiceEvent((Invoice) stripeObject);
                }
                break;
            default:
                // Ignore events not used by this backend.
                break;
        }
    }

    private void handleCheckoutCompleted(Session session) {
        String customerId = trimToNull(session.getCustomer());
        String subscriptionId = trimToNull(session.getSubscription());
        if (customerId == null) {
            return;
        }

        Optional<User> userOpt = userRepository.findByStripeCustomerId(customerId);
        if (userOpt.isEmpty()) {
            String userIdFromMetadata = trimToNull(session.getMetadata() != null ? session.getMetadata().get("userId") : null);
            if (userIdFromMetadata != null) {
                try {
                    Long userId = Long.parseLong(userIdFromMetadata);
                    userOpt = userRepository.findById(userId);
                } catch (NumberFormatException ignored) {
                    // ignore metadata parsing errors
                }
            }
        }
        if (userOpt.isEmpty()) {
            log.warn("Stripe checkout session {} could not be matched to a local user", session.getId());
            return;
        }

        User user = userOpt.get();
        user.setStripeCustomerId(customerId);
        if (subscriptionId != null) {
            user.setStripeSubscriptionId(subscriptionId);
            syncUserFromStripeSubscriptionId(user, subscriptionId);
            log.info("Synced Stripe checkout session {} to user {}", session.getId(), user.getId());
        } else {
            userRepository.save(user);
        }
    }

    private void handleInvoiceEvent(Invoice invoice) {
        String subscriptionId = trimToNull(invoice.getSubscription());
        if (subscriptionId == null) {
            return;
        }

        Optional<User> userOpt = userRepository.findByStripeSubscriptionId(subscriptionId);
        if (userOpt.isEmpty()) {
            log.warn("Stripe invoice {} references unknown subscription {}", invoice.getId(), subscriptionId);
            return;
        }
        syncUserFromStripeSubscriptionId(userOpt.get(), subscriptionId);
    }

    private void syncUserFromSubscription(Subscription subscription) {
        String customerId = trimToNull(subscription.getCustomer());
        String subscriptionId = trimToNull(subscription.getId());
        if (customerId == null && subscriptionId == null) {
            return;
        }

        Optional<User> userOpt = Optional.empty();
        if (subscriptionId != null) {
            userOpt = userRepository.findByStripeSubscriptionId(subscriptionId);
        }
        if (userOpt.isEmpty() && customerId != null) {
            userOpt = userRepository.findByStripeCustomerId(customerId);
        }
        if (userOpt.isEmpty()) {
            log.warn("Stripe subscription {} for customer {} could not be matched to a local user", subscriptionId, customerId);
            return;
        }

        User user = userOpt.get();
        user.setStripeCustomerId(customerId);
        user.setStripeSubscriptionId(subscriptionId);
        applySubscriptionToUser(user, subscription);
        userRepository.save(user);
        log.info("Synced Stripe subscription {} to user {}", subscriptionId, user.getId());
    }

    private void syncUserFromStripeSubscriptionId(User user, String subscriptionId) {
        ensureStripeApiConfigured();
        Stripe.apiKey = stripeSecretKey;
        try {
            Subscription subscription = Subscription.retrieve(subscriptionId);
            user.setStripeSubscriptionId(subscriptionId);
            user.setStripeCustomerId(trimToNull(subscription.getCustomer()));
            applySubscriptionToUser(user, subscription);
            userRepository.save(user);
        } catch (StripeException ex) {
            throw new IllegalStateException("Failed to sync Stripe subscription");
        }
    }

    private void applySubscriptionToUser(User user, Subscription subscription) {
        String status = trimToNull(subscription.getStatus());
        user.setSubscriptionStatus(status);
        user.setSubscriptionCurrentPeriodEnd(toLocalDateTime(subscription.getCurrentPeriodEnd()));
        if (isProSubscriptionStatus(status)) {
            user.setPlan(PLAN_PRO);
        } else {
            user.setPlan(PLAN_FREE);
        }
    }

    private boolean isProSubscriptionStatus(String status) {
        return "active".equalsIgnoreCase(status) || "trialing".equalsIgnoreCase(status);
    }

    private int resolveDailyLimit(User user) {
        String plan = normalizePlan(user.getPlan());
        if (PLAN_PRO.equals(plan)) {
            return dailyLimitPro;
        }
        return dailyLimitFree;
    }

    private long countUsedToday(User user) {
        LocalDate today = LocalDate.now();
        LocalDateTime start = today.atStartOfDay();
        LocalDateTime end = start.plusDays(1);
        return receiptRepository.countByUserAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(user, start, end);
    }

    private int calculateRemainingToday(int dailyLimit, long usedToday) {
        if (dailyLimit < 0) {
            return -1;
        }
        long remaining = (long) dailyLimit - usedToday;
        return (int) Math.max(0, remaining);
    }

    private String normalizePlan(String rawPlan) {
        String plan = trimToNull(rawPlan);
        if (PLAN_PRO.equalsIgnoreCase(plan)) {
            return PLAN_PRO;
        }
        return PLAN_FREE;
    }

    private String ensureStripeCustomerId(User user) {
        String stripeCustomerId = trimToNull(user.getStripeCustomerId());
        if (stripeCustomerId != null) {
            return stripeCustomerId;
        }

        try {
            CustomerCreateParams params = CustomerCreateParams.builder()
                .setEmail(user.getEmail())
                .setName(user.getUsername())
                .build();
            com.stripe.model.Customer customer = com.stripe.model.Customer.create(params);
            stripeCustomerId = customer.getId();
            user.setStripeCustomerId(stripeCustomerId);
            userRepository.save(user);
            return stripeCustomerId;
        } catch (StripeException ex) {
            throw new IllegalStateException("Failed to create Stripe customer");
        }
    }

    private void addPriceId(String priceId) {
        String normalized = trimToNull(priceId);
        if (normalized != null) {
            allowedPriceIds.add(normalized);
        }
    }

    private void ensureStripeApiConfigured() {
        if (stripeSecretKey.isEmpty()) {
            throw new IllegalStateException("Stripe secret key is not configured");
        }
    }

    private void ensureStripeWebhookConfigured() {
        ensureStripeApiConfigured();
        if (stripeWebhookSecret.isEmpty()) {
            throw new IllegalStateException("Stripe webhook secret is not configured");
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

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private LocalDateTime toLocalDateTime(Long epochSeconds) {
        if (epochSeconds == null || epochSeconds <= 0) {
            return null;
        }
        return LocalDateTime.ofInstant(Instant.ofEpochSecond(epochSeconds), ZoneOffset.UTC);
    }
}
