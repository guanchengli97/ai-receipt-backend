package com.example.aireceiptbackend.exception;

public class DailyReceiptLimitExceededException extends RuntimeException {
    public DailyReceiptLimitExceededException(String message) {
        super(message);
    }
}
