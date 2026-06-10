package com.example.aireceiptbackend.service;

import com.example.aireceiptbackend.exception.DailyReceiptLimitExceededException;
import com.example.aireceiptbackend.model.ReceiptDailyUsage;
import com.example.aireceiptbackend.model.User;
import com.example.aireceiptbackend.repository.ReceiptDailyUsageRepository;
import com.example.aireceiptbackend.repository.ReceiptRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
public class ReceiptUsageService {

    private final ReceiptDailyUsageRepository receiptDailyUsageRepository;
    private final ReceiptRepository receiptRepository;

    public ReceiptUsageService(
        ReceiptDailyUsageRepository receiptDailyUsageRepository,
        ReceiptRepository receiptRepository
    ) {
        this.receiptDailyUsageRepository = receiptDailyUsageRepository;
        this.receiptRepository = receiptRepository;
    }

    @Transactional
    public void reserveDailyScanSlot(User user, int dailyLimit) {
        if (dailyLimit < 0) {
            return;
        }

        LocalDate today = LocalDate.now();
        int existingReceiptCount = countExistingReceiptsToday(user, today);
        receiptDailyUsageRepository.ensureUsageRow(user.getId(), today, existingReceiptCount, dailyLimit);

        int updated = receiptDailyUsageRepository.reserveOne(user, today, dailyLimit);
        if (updated == 0) {
            throw new DailyReceiptLimitExceededException(
                String.format("Daily receipt scan limit reached (%d scans/day)", dailyLimit)
            );
        }
    }

    @Transactional
    public void releaseDailyScanSlot(User user) {
        receiptDailyUsageRepository.releaseOne(user, LocalDate.now());
    }

    @Transactional
    public long getUsedToday(User user, int dailyLimit) {
        LocalDate today = LocalDate.now();
        if (dailyLimit < 0) {
            return countExistingReceiptsToday(user, today);
        }

        int existingReceiptCount = countExistingReceiptsToday(user, today);
        receiptDailyUsageRepository.ensureUsageRow(user.getId(), today, existingReceiptCount, dailyLimit);
        return receiptDailyUsageRepository.findByUserAndUsageDate(user, today)
            .map(ReceiptDailyUsage::getUsedCount)
            .map(Integer::longValue)
            .orElse((long) existingReceiptCount);
    }

    private int countExistingReceiptsToday(User user, LocalDate today) {
        LocalDateTime start = today.atStartOfDay();
        LocalDateTime end = start.plusDays(1);
        long count = receiptRepository.countByUserAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(user, start, end);
        return count > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) count;
    }
}
