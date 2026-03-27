package com.example.aireceiptbackend.repository;

import com.example.aireceiptbackend.model.StripeWebhookEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StripeWebhookEventRepository extends JpaRepository<StripeWebhookEvent, Long> {
    boolean existsByEventId(String eventId);
}
