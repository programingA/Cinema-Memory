package com.cinemamemory.api.film;

import com.cinemamemory.api.user.User;
import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
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

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "s3_key", nullable = false, length = 700)
    private String s3Key;

    @Column(name = "cdn_url", nullable = false, length = 700)
    private String cdnUrl;

    @Column(name = "content_type", nullable = false)
    private String contentType;

    @Column(name = "byte_size", nullable = false)
    private long byteSize;

    @Column(name = "thumbnail_url")
    private String thumbnailUrl;

    @Lob
    @Basic(fetch = FetchType.LAZY)
    @Column(name = "media_data", columnDefinition = "LONGBLOB")
    private byte[] mediaData;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected MediaAsset() {
    }

    public MediaAsset(MemoryScene scene, User user, String s3Key, String cdnUrl, String contentType, long byteSize, String thumbnailUrl) {
        this(scene, user, deriveFileName(cdnUrl, s3Key), s3Key, cdnUrl, contentType, byteSize, thumbnailUrl, null);
    }

    public MediaAsset(MemoryScene scene, User user, String s3Key, String cdnUrl, String contentType, long byteSize, String thumbnailUrl, byte[] mediaData) {
        this(scene, user, deriveFileName(cdnUrl, s3Key), s3Key, cdnUrl, contentType, byteSize, thumbnailUrl, mediaData);
    }

    public MediaAsset(MemoryScene scene, User user, String fileName, String s3Key, String cdnUrl, String contentType, long byteSize, String thumbnailUrl) {
        this(scene, user, fileName, s3Key, cdnUrl, contentType, byteSize, thumbnailUrl, null);
    }

    public MediaAsset(MemoryScene scene, User user, String fileName, String s3Key, String cdnUrl, String contentType, long byteSize, String thumbnailUrl, byte[] mediaData) {
        this.scene = scene;
        this.user = user;
        this.fileName = fileName;
        this.s3Key = s3Key;
        this.cdnUrl = cdnUrl;
        this.contentType = contentType;
        this.byteSize = byteSize;
        this.thumbnailUrl = thumbnailUrl;
        this.mediaData = mediaData == null ? null : mediaData.clone();
    }

    @PrePersist
    void prePersist() {
        createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public String getFileName() {
        return fileName;
    }

    public String getS3Key() {
        return s3Key;
    }

    public String getCdnUrl() {
        return cdnUrl;
    }

    public String getContentType() {
        return contentType;
    }

    public long getByteSize() {
        return byteSize;
    }

    public byte[] getMediaData() {
        return mediaData == null ? null : mediaData.clone();
    }

    public void replaceMediaData(byte[] mediaData) {
        this.mediaData = mediaData == null ? null : mediaData.clone();
        this.byteSize = mediaData == null ? 0 : mediaData.length;
    }

    public boolean hasLegacyMediaData() {
        return mediaData != null && mediaData.length > 0;
    }

    public void storeInS3(String s3Key, String contentType, long byteSize, boolean clearLegacyData) {
        this.s3Key = s3Key;
        this.contentType = contentType;
        this.byteSize = byteSize;
        if (clearLegacyData) {
            this.mediaData = null;
        }
    }

    public void clearMediaData() {
        mediaData = null;
    }

    private static String deriveFileName(String cdnUrl, String s3Key) {
        String source = cdnUrl == null || cdnUrl.isBlank() ? s3Key : cdnUrl;
        if (source == null || source.isBlank()) {
            return "media";
        }

        String normalized = source.replace("\\", "/");
        int lastSlash = normalized.lastIndexOf('/');
        String fileName = lastSlash >= 0 ? normalized.substring(lastSlash + 1) : normalized;
        return fileName.isBlank() ? "media" : fileName;
    }
}
