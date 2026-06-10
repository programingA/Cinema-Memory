package com.cinemamemory.api.media;

import com.cinemamemory.api.common.ApiException;
import com.cinemamemory.api.config.AppProperties;
import com.cinemamemory.api.film.MediaAsset;
import com.cinemamemory.api.film.MediaAssetRepository;
import com.cinemamemory.api.film.MemoryScene;
import com.cinemamemory.api.film.MemorySceneRepository;
import com.cinemamemory.api.media.MediaDtos.CompleteUploadRequest;
import com.cinemamemory.api.media.MediaDtos.MediaResponse;
import com.cinemamemory.api.media.MediaDtos.PresignedUrlRequest;
import com.cinemamemory.api.media.MediaDtos.PresignedUrlResponse;
import com.cinemamemory.api.user.User;
import com.cinemamemory.api.user.UserRepository;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.Locale;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

@Service
public class MediaService {
    private final S3Presigner s3Presigner;
    private final AppProperties properties;
    private final UserRepository userRepository;
    private final MemorySceneRepository sceneRepository;
    private final MediaAssetRepository mediaAssetRepository;

    public MediaService(
            S3Presigner s3Presigner,
            AppProperties properties,
            UserRepository userRepository,
            MemorySceneRepository sceneRepository,
            MediaAssetRepository mediaAssetRepository
    ) {
        this.s3Presigner = s3Presigner;
        this.properties = properties;
        this.userRepository = userRepository;
        this.sceneRepository = sceneRepository;
        this.mediaAssetRepository = mediaAssetRepository;
    }

    public PresignedUrlResponse createPresignedUrl(Long userId, PresignedUrlRequest request) {
        String safeName = request.fileName().replaceAll("[^A-Za-z0-9._-]", "_").toLowerCase(Locale.ROOT);
        String s3Key = "users/%d/%s/%s".formatted(userId, UUID.randomUUID(), safeName);

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(properties.aws().s3Bucket())
                .key(s3Key)
                .contentType(request.contentType())
                .contentLength(request.byteSize())
                .build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(10))
                .putObjectRequest(putObjectRequest)
                .build();

        String uploadUrl = s3Presigner.presignPutObject(presignRequest).url().toString();
        return new PresignedUrlResponse(uploadUrl, s3Key, cdnUrl(s3Key));
    }

    @Transactional
    public MediaResponse uploadLocalMedia(Long userId, Long sceneId, MultipartFile file) {
        if (file.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Media file is empty");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));
        MemoryScene scene = sceneRepository.findByIdAndFilmUserId(sceneId, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Scene not found"));

        String originalName = file.getOriginalFilename() == null ? "media" : file.getOriginalFilename();
        String safeName = originalName.replaceAll("[^A-Za-z0-9._-]", "_").toLowerCase(Locale.ROOT);
        String storedName = UUID.randomUUID() + "-" + safeName;
        Path uploadDir = Path.of("uploads", "media").toAbsolutePath().normalize();
        Path target = uploadDir.resolve(storedName).normalize();

        if (!target.startsWith(uploadDir)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid media file name");
        }

        try {
            Files.createDirectories(uploadDir);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException exception) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Media file could not be stored");
        }

        String contentType = file.getContentType() == null ? "application/octet-stream" : file.getContentType();
        String mediaUrl = "/uploads/media/" + storedName;
        MediaAsset media = mediaAssetRepository.save(new MediaAsset(
                scene,
                user,
                "local/" + storedName,
                mediaUrl,
                contentType,
                file.getSize(),
                null
        ));
        return new MediaResponse(media.getId(), media.getCdnUrl());
    }

    @Transactional
    public MediaResponse completeUpload(Long userId, CompleteUploadRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));
        MemoryScene scene = null;
        if (request.sceneId() != null) {
            scene = sceneRepository.findByIdAndFilmUserId(request.sceneId(), userId)
                    .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Scene not found"));
        }

        MediaAsset media = mediaAssetRepository.save(new MediaAsset(
                scene,
                user,
                request.s3Key(),
                request.cdnUrl(),
                request.contentType(),
                request.byteSize(),
                request.thumbnailUrl()
        ));
        return new MediaResponse(media.getId(), media.getCdnUrl());
    }

    @Transactional
    public void deleteMedia(Long userId, Long mediaId) {
        MediaAsset media = mediaAssetRepository.findByIdAndUserId(mediaId, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Media not found"));
        mediaAssetRepository.delete(media);
    }

    private String cdnUrl(String s3Key) {
        String cloudfront = properties.aws().cloudfrontBaseUrl();
        if (cloudfront != null && !cloudfront.isBlank()) {
            return cloudfront.replaceAll("/$", "") + "/" + s3Key;
        }
        return "https://%s.s3.%s.amazonaws.com/%s".formatted(
                properties.aws().s3Bucket(),
                properties.aws().region(),
                s3Key
        );
    }
}
