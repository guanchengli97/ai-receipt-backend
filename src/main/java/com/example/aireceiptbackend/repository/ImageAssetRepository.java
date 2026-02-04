package com.example.aireceiptbackend.repository;

import com.example.aireceiptbackend.model.ImageAsset;
import com.example.aireceiptbackend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ImageAssetRepository extends JpaRepository<ImageAsset, Long> {
    Optional<ImageAsset> findByIdAndUser(Long id, User user);
}
