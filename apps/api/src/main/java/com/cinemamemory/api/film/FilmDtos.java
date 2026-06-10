package com.cinemamemory.api.film;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

public final class FilmDtos {
    private FilmDtos() {
    }

    public record FilmRequest(
            @NotBlank String title,
            String description,
            String coverImageUrl,
            String mood
    ) {
    }

    public record SceneRequest(
            @NotBlank String title,
            @NotBlank String body,
            LocalDate memoryDate,
            String location,
            String mood,
            int sortOrder
    ) {
    }

    public record SceneOrderRequest(@NotNull List<SceneOrderItem> scenes) {
    }

    public record SceneOrderItem(@NotNull Long sceneId, int sortOrder) {
    }

    public record FilmResponse(
            Long id,
            String title,
            String description,
            String coverImageUrl,
            String mood,
            String visibility,
            Instant createdAt,
            int sceneCount
    ) {
        static FilmResponse from(Film film) {
            return new FilmResponse(
                    film.getId(),
                    film.getTitle(),
                    film.getDescription(),
                    film.getCoverImageUrl(),
                    film.getMood(),
                    film.getVisibility().name(),
                    film.getCreatedAt(),
                    film.getScenes().size()
            );
        }
    }

    public record SceneResponse(
            Long id,
            Long filmId,
            String title,
            String body,
            LocalDate memoryDate,
            String location,
            String mood,
            int sortOrder,
            List<String> mediaUrls
    ) {
        static SceneResponse from(MemoryScene scene) {
            return new SceneResponse(
                    scene.getId(),
                    scene.getFilm().getId(),
                    scene.getTitle(),
                    scene.getBody(),
                    scene.getMemoryDate(),
                    scene.getLocation(),
                    scene.getMood(),
                    scene.getSortOrder(),
                    scene.getMediaAssets().stream().map(MediaAsset::getCdnUrl).toList()
            );
        }
    }

    public record PlaybackResponse(
            Long id,
            String title,
            String description,
            String coverImageUrl,
            String mood,
            Instant createdAt,
            int sceneCount,
            List<SceneResponse> scenes
    ) {
        static PlaybackResponse from(Film film) {
            List<SceneResponse> scenes = film.getScenes().stream()
                    .sorted(Comparator.comparingInt(MemoryScene::getSortOrder))
                    .map(SceneResponse::from)
                    .toList();
            return new PlaybackResponse(
                    film.getId(),
                    film.getTitle(),
                    film.getDescription(),
                    film.getCoverImageUrl(),
                    film.getMood(),
                    film.getCreatedAt(),
                    scenes.size(),
                    scenes
            );
        }
    }
}
