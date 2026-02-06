package com.example.aireceiptbackend.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class CategorySpendingStatsResponse {
    private LocalDate monthStart;
    private LocalDate monthEnd;
    private String currency;
    private BigDecimal totalSpent;
    private List<CategorySpending> categories = new ArrayList<>();

    public LocalDate getMonthStart() {
        return monthStart;
    }

    public void setMonthStart(LocalDate monthStart) {
        this.monthStart = monthStart;
    }

    public LocalDate getMonthEnd() {
        return monthEnd;
    }

    public void setMonthEnd(LocalDate monthEnd) {
        this.monthEnd = monthEnd;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public BigDecimal getTotalSpent() {
        return totalSpent;
    }

    public void setTotalSpent(BigDecimal totalSpent) {
        this.totalSpent = totalSpent;
    }

    public List<CategorySpending> getCategories() {
        return categories;
    }

    public void setCategories(List<CategorySpending> categories) {
        this.categories = categories;
    }

    public static class CategorySpending {
        private String category;
        private BigDecimal amount;

        public String getCategory() {
            return category;
        }

        public void setCategory(String category) {
            this.category = category;
        }

        public BigDecimal getAmount() {
            return amount;
        }

        public void setAmount(BigDecimal amount) {
            this.amount = amount;
        }
    }
}
