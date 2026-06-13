package com.cinemamemory.api.media;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public final class MediaDtos {
    private MediaDtos() {
    }

    public record PresignedUrlRequest(
            @NotBlank String fileName,
            @NotBlank String contentType,
            @Min(1) long byteSize
    ) {
    }

    public record PresignedUrlResponse(
            String uploadUrl,
            String s3Key,
            String cdnUrl
    ) {
    }

    public record CompleteUploadRequest(
            Long sceneId,
            @NotBlank String s3Key,
            @NotBlank String cdnUrl,
            @NotBlank String contentType,
            @Min(1) long byteSize,
            String thumbnailUrl
    ) {
    }

    public record MediaResponse(
            Long id,
            String cdnUrl
    ) {
    }

    public record S3MigrationResponse(
            int scanned,
            int uploaded,
            int cleared,
            int skipped,
            int failed
    ) {
    }
}
