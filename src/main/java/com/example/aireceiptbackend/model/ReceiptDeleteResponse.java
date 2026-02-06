package com.example.aireceiptbackend.model;

import java.util.List;

public class ReceiptDeleteResponse {
    private int deletedCount;
    private List<Long> deletedIds;

    public ReceiptDeleteResponse() {
    }

    public ReceiptDeleteResponse(int deletedCount, List<Long> deletedIds) {
        this.deletedCount = deletedCount;
        this.deletedIds = deletedIds;
    }

    public int getDeletedCount() {
        return deletedCount;
    }

    public void setDeletedCount(int deletedCount) {
        this.deletedCount = deletedCount;
    }

    public List<Long> getDeletedIds() {
        return deletedIds;
    }

    public void setDeletedIds(List<Long> deletedIds) {
        this.deletedIds = deletedIds;
    }
}
