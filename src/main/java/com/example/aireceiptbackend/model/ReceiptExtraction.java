package com.example.aireceiptbackend.model;

import java.util.List;

public class ReceiptExtraction {
    private String merchantName;
    private String receiptDate;
    private String currency;
    private String subtotal;
    private String tax;
    private String total;
    private List<ReceiptItemExtraction> items;

    public String getMerchantName() {
        return merchantName;
    }

    public void setMerchantName(String merchantName) {
        this.merchantName = merchantName;
    }

    public String getReceiptDate() {
        return receiptDate;
    }

    public void setReceiptDate(String receiptDate) {
        this.receiptDate = receiptDate;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(String subtotal) {
        this.subtotal = subtotal;
    }

    public String getTax() {
        return tax;
    }

    public void setTax(String tax) {
        this.tax = tax;
    }

    public String getTotal() {
        return total;
    }

    public void setTotal(String total) {
        this.total = total;
    }

    public List<ReceiptItemExtraction> getItems() {
        return items;
    }

    public void setItems(List<ReceiptItemExtraction> items) {
        this.items = items;
    }
}
