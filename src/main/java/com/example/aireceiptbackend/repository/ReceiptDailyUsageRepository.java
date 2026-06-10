package com.example.aireceiptbackend.repository;

import com.example.aireceiptbackend.model.ReceiptDailyUsage;
import com.example.aireceiptbackend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;

public interface ReceiptDailyUsageRepository extends JpaRepository<ReceiptDailyUsage, Long> {

    @Modifying
    @Query(
        value = "insert into receipt_daily_usage " +
            "(user_id, usage_date, used_count, limit_count, created_at, updated_at) " +
            "values (:userId, :usageDate, :initialUsedCount, :limitCount, current_timestamp, current_timestamp) " +
            "on duplicate key update limit_count = values(limit_count), updated_at = current_timestamp",
        nativeQuery = true
    )
    int ensureUsageRow(
        @Param("userId") Long userId,
        @Param("usageDate") LocalDate usageDate,
        @Param("initialUsedCount") int initialUsedCount,
        @Param("limitCount") int limitCount
    );

    @Modifying
    @Query(
        "update ReceiptDailyUsage u " +
            "set u.usedCount = u.usedCount + 1, " +
            "u.limitCount = :limitCount, " +
            "u.updatedAt = current_timestamp " +
            "where u.user = :user " +
            "and u.usageDate = :usageDate " +
            "and u.usedCount < :limitCount"
    )
    int reserveOne(
        @Param("user") User user,
        @Param("usageDate") LocalDate usageDate,
        @Param("limitCount") int limitCount
    );

    @Modifying
    @Query(
        "update ReceiptDailyUsage u " +
            "set u.usedCount = u.usedCount - 1, " +
            "u.updatedAt = current_timestamp " +
            "where u.user = :user " +
            "and u.usageDate = :usageDate " +
            "and u.usedCount > 0"
    )
    int releaseOne(@Param("user") User user, @Param("usageDate") LocalDate usageDate);
}
