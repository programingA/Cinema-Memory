package com.cinemamemory.api.film;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface MediaAssetRepository extends JpaRepository<MediaAsset, Long> {
    Optional<MediaAsset> findByIdAndUserId(Long id, Long userId);

    Optional<MediaAsset> findFirstByCdnUrl(String cdnUrl);

    Optional<MediaAsset> findFirstByFileName(String fileName);

    @Query("""
            select media
            from MediaAsset media
            where media.mediaData is not null
              and (media.s3Key is null or media.s3Key like 'local/%')
            order by media.id asc
            """)
    List<MediaAsset> findLegacyMediaDataToMigrate(Pageable pageable);

    @Query("""
            select media
            from MediaAsset media
            where media.mediaData is not null
            order by media.id asc
            """)
    List<MediaAsset> findMediaDataToClear(Pageable pageable);

    @Query("select media.cdnUrl from MediaAsset media where media.user.id = :userId")
    List<String> findCdnUrlsByUserId(Long userId);

    @Modifying
    @Query("delete from MediaAsset media where media.user.id = :userId")
    int deleteByUserId(Long userId);
}
