package com.example.aireceiptbackend.model;

import java.time.LocalDateTime;

public class BillingUsageResponse {
    private String plan;
    private String subscriptionStatus;
    private LocalDateTime subscriptionCurrentPeriodEnd;
    private Boolean cancelAtPeriodEnd;
    private Integer dailyLimit;
    private Long usedToday;
    private Integer remainingToday;

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

    public Boolean getCancelAtPeriodEnd() {
        return cancelAtPeriodEnd;
    }

    public void setCancelAtPeriodEnd(Boolean cancelAtPeriodEnd) {
        this.cancelAtPeriodEnd = cancelAtPeriodEnd;
    }

    public Integer getDailyLimit() {
        return dailyLimit;
    }

    public void setDailyLimit(Integer dailyLimit) {
        this.dailyLimit = dailyLimit;
    }

    public Long getUsedToday() {
        return usedToday;
    }

    public void setUsedToday(Long usedToday) {
        this.usedToday = usedToday;
    }

    public Integer getRemainingToday() {
        return remainingToday;
    }

    public void setRemainingToday(Integer remainingToday) {
        this.remainingToday = remainingToday;
    }
}
