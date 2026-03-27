package com.example.aireceiptbackend.model;

import java.time.LocalDateTime;

public class BillingStatusResponse {
    private String plan;
    private String subscriptionStatus;
    private LocalDateTime subscriptionCurrentPeriodEnd;
    private Integer dailyLimit;

    public String getPlan() {
        return plan;
    }

    public void setPlan(String plan) {
        this.plan = plan;
    }

    public String getSubscriptionStatus() {
        return subscriptionStatus;
    }

    public void setSubscriptionStatus(String subscriptionStatus) {
        this.subscriptionStatus = subscriptionStatus;
    }

    public LocalDateTime getSubscriptionCurrentPeriodEnd() {
        return subscriptionCurrentPeriodEnd;
    }

    public void setSubscriptionCurrentPeriodEnd(LocalDateTime subscriptionCurrentPeriodEnd) {
        this.subscriptionCurrentPeriodEnd = subscriptionCurrentPeriodEnd;
    }

    public Integer getDailyLimit() {
        return dailyLimit;
    }

    public void setDailyLimit(Integer dailyLimit) {
        this.dailyLimit = dailyLimit;
    }
}
