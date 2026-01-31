package com.example.aireceiptbackend.repository;

import com.example.aireceiptbackend.model.Receipt;
import com.example.aireceiptbackend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReceiptRepository extends JpaRepository<Receipt, Long> {
    List<Receipt> findByUser(User user);
}
