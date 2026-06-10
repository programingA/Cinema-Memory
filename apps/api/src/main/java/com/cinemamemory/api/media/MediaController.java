package com.cinemamemory.api.media;

import com.cinemamemory.api.media.MediaDtos.CompleteUploadRequest;
import com.cinemamemory.api.media.MediaDtos.MediaResponse;
import com.cinemamemory.api.media.MediaDtos.PresignedUrlRequest;
import com.cinemamemory.api.media.MediaDtos.PresignedUrlResponse;
import com.cinemamemory.api.security.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/media")
public class MediaController {
    private final MediaService mediaService;

    public MediaController(MediaService mediaService) {
        this.mediaService = mediaService;
    }

    @PostMapping("/presigned-url")
    PresignedUrlResponse createPresignedUrl(@CurrentUser Long userId, @Valid @RequestBody PresignedUrlRequest request) {
        return mediaService.createPresignedUrl(userId, request);
    }

    @PostMapping("/complete")
    MediaResponse completeUpload(@CurrentUser Long userId, @Valid @RequestBody CompleteUploadRequest request) {
        return mediaService.completeUpload(userId, request);
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    MediaResponse uploadMedia(
            @CurrentUser Long userId,
            @RequestParam Long sceneId,
            @RequestParam MultipartFile file
    ) {
        return mediaService.uploadLocalMedia(userId, sceneId, file);
    }

    @DeleteMapping("/{mediaId}")
    ResponseEntity<Void> deleteMedia(@CurrentUser Long userId, @PathVariable Long mediaId) {
        mediaService.deleteMedia(userId, mediaId);
        return ResponseEntity.noContent().build();
    }
}
