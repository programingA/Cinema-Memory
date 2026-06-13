package com.cinemamemory.api.media;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import com.cinemamemory.api.config.AppProperties;
import com.cinemamemory.api.film.MediaAsset;
import com.cinemamemory.api.film.MediaAssetRepository;
import com.cinemamemory.api.user.User;
import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;

@ExtendWith(MockitoExtension.class)
class LocalMediaControllerTest {
    @Mock
    private MediaAssetRepository mediaAssetRepository;

    @Mock
    private S3Client s3Client;

    private LocalMediaController controller;

    @BeforeEach
    void setUp() {
        controller = new LocalMediaController(mediaAssetRepository, s3Client, appProperties());
    }

    @Test
    void localMediaStreamsS3ObjectWithContentType() throws Exception {
        byte[] mediaBytes = "jpeg".getBytes();
        MediaAsset media = new MediaAsset(
                null,
                new User("user@example.com", "hash", "User", null),
                "photo.jpg",
                "uploads/media/users/1/photo.jpg",
                "/uploads/media/photo.jpg",
                "image/jpeg",
                mediaBytes.length,
                null
        );
        ResponseInputStream<GetObjectResponse> s3Object = new ResponseInputStream<>(
                GetObjectResponse.builder()
                        .contentType("image/jpeg")
                        .contentLength((long) mediaBytes.length)
                        .build(),
                AbortableInputStream.create(new ByteArrayInputStream(mediaBytes))
        );

        given(mediaAssetRepository.findFirstByFileName("photo.jpg")).willReturn(Optional.of(media));
        given(s3Client.getObject(any(GetObjectRequest.class))).willReturn(s3Object);

        ResponseEntity<Resource> response = controller.localMedia("photo.jpg");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.IMAGE_JPEG);
        assertThat(response.getHeaders().getContentLength()).isEqualTo(mediaBytes.length);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getInputStream().readAllBytes()).isEqualTo(mediaBytes);
    }

    @Test
    void localMediaFallsBackToLegacyMediaDataWhenS3ObjectIsMissing() throws Exception {
        byte[] mediaBytes = "png".getBytes();
        MediaAsset media = new MediaAsset(
                null,
                new User("user@example.com", "hash", "User", null),
                "legacy.png",
                "uploads/media/users/1/legacy.png",
                "/uploads/media/legacy.png",
                "image/png",
                mediaBytes.length,
                null,
                mediaBytes
        );

        given(mediaAssetRepository.findFirstByFileName("legacy.png")).willReturn(Optional.of(media));
        given(s3Client.getObject(any(GetObjectRequest.class)))
                .willThrow(NoSuchKeyException.builder().message("missing").build());

        ResponseEntity<Resource> response = controller.localMedia("legacy.png");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.IMAGE_PNG);
        assertThat(response.getHeaders().getContentLength()).isEqualTo(mediaBytes.length);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getInputStream().readAllBytes()).isEqualTo(mediaBytes);
    }

    private AppProperties appProperties() {
        return new AppProperties(
                "http://localhost:3000",
                new AppProperties.Admin("admin@example.com", "password", "Admin"),
                new AppProperties.Jwt("cinema-memory", "secret", 15, 14),
                new AppProperties.Aws("ap-northeast-2", "media-bucket", "uploads/media", ""),
                new AppProperties.Security(List.of("http://localhost:3000"), false, 5),
                new AppProperties.Media(52_428_800, "s3")
        );
    }
}
