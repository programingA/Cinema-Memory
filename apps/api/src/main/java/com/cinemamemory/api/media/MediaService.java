package com.cinemamemory.api.media;

import com.cinemamemory.api.common.ApiException;
import com.cinemamemory.api.common.InputSanitizer;
import com.cinemamemory.api.config.AppProperties;
import com.cinemamemory.api.film.MediaAsset;
import com.cinemamemory.api.film.MediaAssetRepository;
import com.cinemamemory.api.film.MemoryScene;
import com.cinemamemory.api.film.MemorySceneRepository;
import com.cinemamemory.api.media.MediaDtos.CompleteUploadRequest;
import com.cinemamemory.api.media.MediaDtos.MediaResponse;
import com.cinemamemory.api.media.MediaDtos.PresignedUrlRequest;
import com.cinemamemory.api.media.MediaDtos.PresignedUrlResponse;
import com.cinemamemory.api.media.MediaDtos.S3MigrationResponse;
import com.cinemamemory.api.user.User;
import com.cinemamemory.api.user.UserRepository;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

@Service
public class MediaService {
    private static final int MAX_FILE_NAME_LENGTH = 255;
    private static final int UUID_PREFIX_LENGTH = 37;
    private static final int MAX_SAFE_NAME_LENGTH = MAX_FILE_NAME_LENGTH - UUID_PREFIX_LENGTH;
    private static final Set<String> ALLOWED_MEDIA_TYPES = Set.of(
            "image/jpeg",
            "image/jpg",
            "image/png",
            "image/gif",
            "image/webp",
            "video/mp4",
            "video/webm",
            "video/ogg",
            "video/quicktime"
    );
    private static final Map<String, String> MEDIA_TYPE_BY_EXTENSION = Map.ofEntries(
            Map.entry("gif", "image/gif"),
            Map.entry("jpeg", "image/jpeg"),
            Map.entry("jpg", "image/jpeg"),
            Map.entry("m4v", "video/mp4"),
            Map.entry("mov", "video/quicktime"),
            Map.entry("mp4", "video/mp4"),
            Map.entry("ogg", "video/ogg"),
            Map.entry("png", "image/png"),
            Map.entry("webm", "video/webm"),
            Map.entry("webp", "image/webp")
    );

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final AppProperties properties;
    private final UserRepository userRepository;
    private final MemorySceneRepository sceneRepository;
    private final MediaAssetRepository mediaAssetRepository;

    public MediaService(
            S3Client s3Client,
            S3Presigner s3Presigner,
            AppProperties properties,
            UserRepository userRepository,
            MemorySceneRepository sceneRepository,
            MediaAssetRepository mediaAssetRepository
    ) {
        this.s3Client = s3Client;
        this.s3Presigner = s3Presigner;
        this.properties = properties;
        this.userRepository = userRepository;
        this.sceneRepository = sceneRepository;
        this.mediaAssetRepository = mediaAssetRepository;
    }

    public PresignedUrlResponse createPresignedUrl(Long userId, PresignedUrlRequest request) {
        String contentType = validateContentType(request.contentType(), request.fileName());
        validateMediaSize(request.byteSize());
        ensureS3Configured();

        String storedName = storedFileName(request.fileName());
        String s3Key = s3ObjectKey(userId, storedName);

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(properties.aws().s3Bucket())
                .key(s3Key)
                .contentType(contentType)
                .contentLength(request.byteSize())
                .build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(10))
                .putObjectRequest(putObjectRequest)
                .build();

        String uploadUrl = s3Presigner.presignPutObject(presignRequest).url().toString();
        return new PresignedUrlResponse(uploadUrl, s3Key, mediaUrl(storedName));
    }

    @Transactional
    public MediaResponse uploadLocalMedia(Long userId, Long sceneId, MultipartFile file) {
        if (file.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Media file is empty");
        }
        String originalName = file.getOriginalFilename() == null ? "media" : file.getOriginalFilename();
        String contentType = validateContentType(file.getContentType(), originalName);
        byte[] mediaBytes = readMultipartBytes(file);
        validateMediaSize(mediaBytes.length);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));
        MemoryScene scene = sceneRepository.findByIdAndFilmUserId(sceneId, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Scene not found"));

        String storedName = storedFileName(originalName);
        String storageKey = storeMediaBytes(userId, storedName, contentType, mediaBytes);
        MediaAsset media = mediaAssetRepository.save(new MediaAsset(
                scene,
                user,
                storedName,
                storageKey,
                mediaUrl(storedName),
                contentType,
                mediaBytes.length,
                null
        ));
        return new MediaResponse(media.getId(), media.getCdnUrl());
    }

    @Transactional
    public MediaResponse completeUpload(Long userId, CompleteUploadRequest request) {
        String s3Key = validateS3Key(userId, request.s3Key());
        InputSanitizer.optionalMediaUrl(request.cdnUrl());
        String contentType = validateContentType(request.contentType(), s3Key);
        validateMediaSize(request.byteSize());

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));
        MemoryScene scene = null;
        if (request.sceneId() != null) {
            scene = sceneRepository.findByIdAndFilmUserId(request.sceneId(), userId)
                    .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Scene not found"));
        }

        String storedName = fileNameFromKey(s3Key);
        MediaAsset media = mediaAssetRepository.save(new MediaAsset(
                scene,
                user,
                storedName,
                s3Key,
                mediaUrl(storedName),
                contentType,
                request.byteSize(),
                InputSanitizer.optionalMediaUrl(request.thumbnailUrl())
        ));
        return new MediaResponse(media.getId(), media.getCdnUrl());
    }

    @Transactional
    public void deleteMedia(Long userId, Long mediaId) {
        MediaAsset media = mediaAssetRepository.findByIdAndUserId(mediaId, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Media not found"));
        mediaAssetRepository.delete(media);
    }

    @Transactional
    public void backfillLocalMedia(String cdnUrl, MultipartFile file) {
        if (file.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Media file is empty");
        }

        String originalName = file.getOriginalFilename() == null ? "media" : file.getOriginalFilename();
        String contentType = validateContentType(file.getContentType(), originalName);
        byte[] mediaBytes = readMultipartBytes(file);
        validateMediaSize(mediaBytes.length);

        String normalizedUrl = InputSanitizer.optionalMediaUrl(cdnUrl);
        if (normalizedUrl == null || !normalizedUrl.startsWith("/uploads/media/")) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid media URL");
        }

        MediaAsset media = mediaAssetRepository.findFirstByCdnUrl(normalizedUrl)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Media not found"));
        String storedName = media.getFileName();
        String storageKey = storeMediaBytes(media.getUser().getId(), storedName, contentType, mediaBytes);
        media.storeInS3(storageKey, contentType, mediaBytes.length, true);
    }

    @Transactional
    public S3MigrationResponse migrateLegacyMediaToS3(int limit, boolean clearLegacyData) {
        int pageSize = Math.max(1, Math.min(limit, 1000));
        List<MediaAsset> legacyMedia = clearLegacyData
                ? mediaAssetRepository.findMediaDataToClear(PageRequest.of(0, pageSize))
                : mediaAssetRepository.findLegacyMediaDataToMigrate(PageRequest.of(0, pageSize));
        int uploaded = 0;
        int cleared = 0;
        int skipped = 0;
        int failed = 0;

        for (MediaAsset media : legacyMedia) {
            byte[] mediaBytes = media.getMediaData();
            if (mediaBytes == null || mediaBytes.length == 0) {
                skipped++;
                continue;
            }

            if (isS3BackedKey(media.getS3Key())) {
                try {
                    verifyS3ObjectExists(media.getS3Key());
                    media.clearMediaData();
                    cleared++;
                } catch (RuntimeException exception) {
                    failed++;
                }
                continue;
            }

            try {
                String s3Key = s3ObjectKey(media.getUser().getId(), media.getFileName());
                putS3Object(s3Key, media.getContentType(), mediaBytes);
                media.storeInS3(s3Key, media.getContentType(), mediaBytes.length, clearLegacyData);
                uploaded++;
                if (clearLegacyData) {
                    cleared++;
                }
            } catch (RuntimeException exception) {
                failed++;
            }
        }

        return new S3MigrationResponse(legacyMedia.size(), uploaded, cleared, skipped, failed);
    }

    private String storeMediaBytes(Long userId, String storedName, String contentType, byte[] mediaBytes) {
        if (usesLocalStorage()) {
            storeLocalMediaFile(storedName, mediaBytes);
            return "local/" + storedName;
        }

        String s3Key = s3ObjectKey(userId, storedName);
        putS3Object(s3Key, contentType, mediaBytes);
        return s3Key;
    }

    private void putS3Object(String s3Key, String contentType, byte[] mediaBytes) {
        ensureS3Configured();
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(properties.aws().s3Bucket())
                .key(s3Key)
                .contentType(contentType)
                .contentLength((long) mediaBytes.length)
                .build();
        s3Client.putObject(putObjectRequest, RequestBody.fromBytes(mediaBytes));
    }

    private void verifyS3ObjectExists(String s3Key) {
        ensureS3Configured();
        s3Client.headObject(HeadObjectRequest.builder()
                .bucket(properties.aws().s3Bucket())
                .key(s3Key)
                .build());
    }

    private void storeLocalMediaFile(String storedName, byte[] mediaBytes) {
        Path uploadDir = Path.of("uploads", "media").toAbsolutePath().normalize();
        Path target = uploadDir.resolve(storedName).normalize();

        if (!target.startsWith(uploadDir)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid media file name");
        }

        try {
            Files.createDirectories(uploadDir);
            Files.write(target, mediaBytes);
        } catch (IOException exception) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Media file could not be stored");
        }
    }

    private String validateContentType(String contentType, String fileName) {
        if (contentType != null && !contentType.isBlank()) {
            String normalized = contentType.trim().toLowerCase(Locale.ROOT);
            if (ALLOWED_MEDIA_TYPES.contains(normalized)) {
                return normalized.equals("image/jpg") ? "image/jpeg" : normalized;
            }
        }

        String extension = extension(fileName);
        String inferred = MEDIA_TYPE_BY_EXTENSION.get(extension);
        if (inferred != null) {
            return inferred;
        }

        throw new ApiException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Unsupported media type");
    }

    private void validateMediaSize(long byteSize) {
        if (byteSize <= 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Media file is empty");
        }

        long maxUploadBytes = maxUploadBytes();
        if (byteSize > maxUploadBytes) {
            throw new ApiException(HttpStatus.PAYLOAD_TOO_LARGE, "Media file is too large");
        }
    }

    private long maxUploadBytes() {
        AppProperties.Media media = properties.media();
        return media == null ? 50 * 1024 * 1024 : media.effectiveMaxUploadBytes();
    }

    private boolean usesLocalStorage() {
        AppProperties.Media media = properties.media();
        return media != null && media.usesLocalStorage();
    }

    private boolean isS3BackedKey(String s3Key) {
        return s3Key != null && !s3Key.isBlank() && !s3Key.startsWith("local/");
    }

    private String storedFileName(String originalName) {
        String safeName = originalName == null ? "media" : originalName.replaceAll("[^A-Za-z0-9._-]", "_").toLowerCase(Locale.ROOT);
        if (safeName.isBlank()) {
            safeName = "media";
        }
        safeName = truncateSafeName(safeName);
        return UUID.randomUUID() + "-" + safeName;
    }

    private String truncateSafeName(String safeName) {
        if (safeName.length() <= MAX_SAFE_NAME_LENGTH) {
            return safeName;
        }

        String extension = extension(safeName);
        if (!extension.isBlank() && extension.length() <= 16) {
            String suffix = "." + extension;
            int baseLength = Math.max(1, MAX_SAFE_NAME_LENGTH - suffix.length());
            return safeName.substring(0, Math.min(baseLength, safeName.length())) + suffix;
        }

        return safeName.substring(0, MAX_SAFE_NAME_LENGTH);
    }

    private String mediaUrl(String fileName) {
        return "/uploads/media/" + fileName;
    }

    private String s3ObjectKey(Long userId, String fileName) {
        String key = "users/%d/%s".formatted(userId, fileName);
        String prefix = normalizedS3Prefix();
        return prefix.isBlank() ? key : prefix + "/" + key;
    }

    private String normalizedS3Prefix() {
        String prefix = properties.aws().s3Prefix();
        if (prefix == null || prefix.isBlank()) {
            return "";
        }
        return prefix.replace("\\", "/").replaceAll("^/+", "").replaceAll("/+$", "");
    }

    private String extension(String fileName) {
        if (fileName == null) {
            return "";
        }

        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == fileName.length() - 1) {
            return "";
        }

        return fileName.substring(dotIndex + 1).trim().toLowerCase(Locale.ROOT);
    }

    private byte[] readMultipartBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException exception) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Media file could not be read");
        }
    }

    private String validateS3Key(Long userId, String s3Key) {
        if (s3Key == null || s3Key.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid media key");
        }

        String sanitized = s3Key.trim();
        String requiredPrefix = s3ObjectKey(userId, "");
        String legacyPrefix = "users/%d/".formatted(userId);
        if (sanitized.startsWith("/")
                || sanitized.contains("\\")
                || sanitized.contains("..")
                || (!sanitized.startsWith(requiredPrefix) && !sanitized.startsWith(legacyPrefix))) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid media key");
        }

        return sanitized;
    }

    private String fileNameFromKey(String s3Key) {
        int slashIndex = s3Key.lastIndexOf('/');
        String fileName = slashIndex >= 0 ? s3Key.substring(slashIndex + 1) : s3Key;
        if (fileName.isBlank() || fileName.contains("\\") || fileName.contains("..")) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid media key");
        }
        return fileName;
    }

    private void ensureS3Configured() {
        String bucket = properties.aws().s3Bucket();
        if (bucket == null || bucket.isBlank()) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Media S3 bucket is not configured");
        }
    }
}
