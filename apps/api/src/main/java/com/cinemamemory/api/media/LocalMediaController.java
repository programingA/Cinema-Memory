package com.cinemamemory.api.media;

import com.cinemamemory.api.common.ApiException;
import com.cinemamemory.api.config.AppProperties;
import com.cinemamemory.api.film.MediaAsset;
import com.cinemamemory.api.film.MediaAssetRepository;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.S3Exception;

@RestController
public class LocalMediaController {
    private final MediaAssetRepository mediaAssetRepository;
    private final S3Client s3Client;
    private final AppProperties properties;

    public LocalMediaController(MediaAssetRepository mediaAssetRepository, S3Client s3Client, AppProperties properties) {
        this.mediaAssetRepository = mediaAssetRepository;
        this.s3Client = s3Client;
        this.properties = properties;
    }

    @GetMapping("/uploads/media/{fileName:.+}")
    ResponseEntity<Resource> localMedia(@PathVariable String fileName) {
        if (fileName.contains("/") || fileName.contains("\\") || fileName.contains("..")) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Media not found");
        }

        String mediaUrl = "/uploads/media/" + fileName;
        MediaAsset media = mediaAssetRepository.findFirstByFileName(fileName)
                .or(() -> mediaAssetRepository.findFirstByCdnUrl(mediaUrl))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Media not found"));

        if (isS3Key(media.getS3Key())) {
            return s3Response(media);
        }

        ResponseEntity<Resource> localResponse = localFileResponse(fileName, mediaUrl);
        if (localResponse != null) {
            return localResponse;
        }

        return legacyMediaDataResponse(media);
    }

    private ResponseEntity<Resource> s3Response(MediaAsset media) {
        String bucket = properties.aws().s3Bucket();
        if (bucket == null || bucket.isBlank()) {
            if (media.hasLegacyMediaData()) {
                return legacyMediaDataResponse(media);
            }
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Media S3 bucket is not configured");
        }

        try {
            ResponseInputStream<GetObjectResponse> object = s3Client.getObject(GetObjectRequest.builder()
                    .bucket(bucket)
                    .key(media.getS3Key())
                    .build());
            GetObjectResponse response = object.response();
            String contentType = response.contentType() == null ? media.getContentType() : response.contentType();
            long contentLength = response.contentLength() == null ? media.getByteSize() : response.contentLength();
            return fileResponse(new InputStreamResource(object), mediaType(contentType), contentLength);
        } catch (NoSuchKeyException exception) {
            return legacyMediaDataResponse(media);
        } catch (S3Exception exception) {
            if (exception.statusCode() == 404 && media.hasLegacyMediaData()) {
                return legacyMediaDataResponse(media);
            }
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Media file could not be read");
        }
    }

    private ResponseEntity<Resource> localFileResponse(String fileName, String mediaUrl) {
        Path uploadDir = Path.of("uploads", "media").toAbsolutePath().normalize();
        Path target = uploadDir.resolve(fileName).normalize();
        if (!target.startsWith(uploadDir)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Media not found");
        }

        if (Files.isRegularFile(target)) {
            return fileResponse(new FileSystemResource(target), contentType(mediaUrl, target), fileSize(target));
        }

        return null;
    }

    private ResponseEntity<Resource> legacyMediaDataResponse(MediaAsset media) {
        byte[] mediaData = media.getMediaData();
        if (mediaData == null || mediaData.length == 0) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Media not found");
        }

        return fileResponse(new ByteArrayResource(mediaData), mediaType(media.getContentType()), mediaData.length);
    }

    private ResponseEntity<Resource> fileResponse(Resource resource, MediaType mediaType, long contentLength) {
        return ResponseEntity.ok()
                .contentType(mediaType)
                .contentLength(contentLength)
                .cacheControl(CacheControl.maxAge(Duration.ofDays(30)).cachePublic())
                .body(resource);
    }

    private boolean isS3Key(String s3Key) {
        return s3Key != null && !s3Key.isBlank() && !s3Key.startsWith("local/");
    }

    private MediaType contentType(String mediaUrl, Path target) {
        return mediaAssetRepository.findFirstByCdnUrl(mediaUrl)
                .map(MediaAsset::getContentType)
                .map(this::mediaType)
                .orElseGet(() -> probeContentType(target));
    }

    private MediaType mediaType(String contentType) {
        try {
            return MediaType.parseMediaType(contentType);
        } catch (RuntimeException exception) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }

    private MediaType probeContentType(Path target) {
        try {
            String probed = Files.probeContentType(target);
            return probed == null ? MediaType.APPLICATION_OCTET_STREAM : mediaType(probed);
        } catch (IOException exception) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }

    private long fileSize(Path target) {
        try {
            return Files.size(target);
        } catch (IOException exception) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Media not found");
        }
    }
}
