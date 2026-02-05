package com.example.aireceiptbackend.repository;

import com.example.aireceiptbackend.model.Receipt;
import com.example.aireceiptbackend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ReceiptRepository extends JpaRepository<Receipt, Long> {
    List<Receipt> findByUser(User user);
    Optional<Receipt> findByIdAndUser(Long id, User user);
    List<Receipt> findByUserEmailOrderByCreatedAtDesc(String email);
    List<Receipt> findByUserUsernameOrderByCreatedAtDesc(String username);
    long countByUserAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(User user, LocalDateTime start, LocalDateTime end);

    @Query("select coalesce(sum(r.totalAmount), 0) from Receipt r where r.user = :user and r.createdAt >= :start and r.createdAt < :end")
    BigDecimal sumTotalAmountByUserAndCreatedAtBetween(
        @Param("user") User user,
        @Param("start") LocalDateTime start,
        @Param("end") LocalDateTime end
    );
}
