package com.cinemamemory.api.media;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.cinemamemory.api.config.AppProperties;
import com.cinemamemory.api.film.Film;
import com.cinemamemory.api.film.MediaAsset;
import com.cinemamemory.api.film.MediaAssetRepository;
import com.cinemamemory.api.film.MemoryScene;
import com.cinemamemory.api.film.MemorySceneRepository;
import com.cinemamemory.api.media.MediaDtos.MediaResponse;
import com.cinemamemory.api.user.User;
import com.cinemamemory.api.user.UserRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@ExtendWith(MockitoExtension.class)
class MediaServiceTest {
    @Mock
    private S3Client s3Client;

    @Mock
    private S3Presigner s3Presigner;

    @Mock
    private UserRepository userRepository;

    @Mock
    private MemorySceneRepository sceneRepository;

    @Mock
    private MediaAssetRepository mediaAssetRepository;

    @Test
    void uploadLocalMediaStoresNewUploadInS3AndKeepsDbBinaryEmpty() {
        AppProperties properties = appProperties("s3");
        MediaService mediaService = new MediaService(
                s3Client,
                s3Presigner,
                properties,
                userRepository,
                sceneRepository,
                mediaAssetRepository
        );
        User user = new User("user@example.com", "hash", "User", null);
        MemoryScene scene = new MemoryScene(new Film(user, "Film", null, null, null), "Scene", "Body", null, null, null, 1);
        byte[] mediaBytes = "jpeg".getBytes();
        MockMultipartFile file = new MockMultipartFile("file", "Sample.JPG", "image/jpeg", mediaBytes);

        given(userRepository.findById(7L)).willReturn(Optional.of(user));
        given(sceneRepository.findByIdAndFilmUserId(11L, 7L)).willReturn(Optional.of(scene));
        given(mediaAssetRepository.save(any(MediaAsset.class))).willAnswer(invocation -> invocation.getArgument(0));

        MediaResponse response = mediaService.uploadLocalMedia(7L, 11L, file);

        ArgumentCaptor<PutObjectRequest> putRequestCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client).putObject(putRequestCaptor.capture(), any(RequestBody.class));
        PutObjectRequest putRequest = putRequestCaptor.getValue();
        assertThat(putRequest.bucket()).isEqualTo("media-bucket");
        assertThat(putRequest.key()).startsWith("uploads/media/users/7/");
        assertThat(putRequest.key()).endsWith("-sample.jpg");
        assertThat(putRequest.contentType()).isEqualTo("image/jpeg");
        assertThat(putRequest.contentLength()).isEqualTo((long) mediaBytes.length);

        ArgumentCaptor<MediaAsset> mediaCaptor = ArgumentCaptor.forClass(MediaAsset.class);
        verify(mediaAssetRepository).save(mediaCaptor.capture());
        MediaAsset saved = mediaCaptor.getValue();
        assertThat(saved.getFileName()).endsWith("-sample.jpg");
        assertThat(saved.getS3Key()).isEqualTo(putRequest.key());
        assertThat(saved.getCdnUrl()).isEqualTo("/uploads/media/" + saved.getFileName());
        assertThat(saved.getMediaData()).isNull();
        assertThat(response.cdnUrl()).isEqualTo(saved.getCdnUrl());
    }

    private AppProperties appProperties(String storageMode) {
        return new AppProperties(
                "http://localhost:3000",
                new AppProperties.Admin("admin@example.com", "password", "Admin"),
                new AppProperties.Jwt("cinema-memory", "secret", 15, 14),
                new AppProperties.Aws("ap-northeast-2", "media-bucket", "uploads/media", ""),
                new AppProperties.Security(List.of("http://localhost:3000"), false, 5),
                new AppProperties.Media(52_428_800, storageMode)
        );
    }
}
