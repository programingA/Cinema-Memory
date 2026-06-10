package com.cinemamemory.api.film;

import com.cinemamemory.api.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "media_assets")
public class MediaAsset {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scene_id")
    private MemoryScene scene;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "s3_key", nullable = false)
    private String s3Key;

    @Column(name = "cdn_url", nullable = false)
    private String cdnUrl;

    @Column(name = "content_type", nullable = false)
    private String contentType;

    @Column(name = "byte_size", nullable = false)
    private long byteSize;

    @Column(name = "thumbnail_url")
    private String thumbnailUrl;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected MediaAsset() {
    }

    public MediaAsset(MemoryScene scene, User user, String s3Key, String cdnUrl, String contentType, long byteSize, String thumbnailUrl) {
        this.scene = scene;
        this.user = user;
        this.s3Key = s3Key;
        this.cdnUrl = cdnUrl;
        this.contentType = contentType;
        this.byteSize = byteSize;
        this.thumbnailUrl = thumbnailUrl;
    }

    @PrePersist
    void prePersist() {
        createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getCdnUrl() {
        return cdnUrl;
    }
}
