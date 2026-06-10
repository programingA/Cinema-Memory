package com.cinemamemory.api.film;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MediaAssetRepository extends JpaRepository<MediaAsset, Long> {
    Optional<MediaAsset> findByIdAndUserId(Long id, Long userId);
}
