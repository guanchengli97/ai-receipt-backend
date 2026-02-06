package com.example.aireceiptbackend.model;

import java.util.List;

public class ReceiptDeleteRequest {
    private List<Long> ids;

    public ReceiptDeleteRequest() {
    }

    public ReceiptDeleteRequest(List<Long> ids) {
        this.ids = ids;
    }

    public List<Long> getIds() {
        return ids;
    }

    public void setIds(List<Long> ids) {
        this.ids = ids;
    }
}
