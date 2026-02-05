package com.example.aireceiptbackend.model;

import java.math.BigDecimal;

public class ReceiptStatsResponse {
    private BigDecimal totalSpentThisMonth;
    private long receiptsProcessedThisMonth;

    public BigDecimal getTotalSpentThisMonth() {
        return totalSpentThisMonth;
    }

    public void setTotalSpentThisMonth(BigDecimal totalSpentThisMonth) {
        this.totalSpentThisMonth = totalSpentThisMonth;
    }

    public long getReceiptsProcessedThisMonth() {
        return receiptsProcessedThisMonth;
    }

    public void setReceiptsProcessedThisMonth(long receiptsProcessedThisMonth) {
        this.receiptsProcessedThisMonth = receiptsProcessedThisMonth;
    }
}
