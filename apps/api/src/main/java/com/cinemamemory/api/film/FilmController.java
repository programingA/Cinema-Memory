package com.cinemamemory.api.film;

import com.cinemamemory.api.film.FilmDtos.FilmRequest;
import com.cinemamemory.api.film.FilmDtos.FilmResponse;
import com.cinemamemory.api.film.FilmDtos.PlaybackResponse;
import com.cinemamemory.api.film.FilmDtos.SceneOrderRequest;
import com.cinemamemory.api.film.FilmDtos.SceneRequest;
import com.cinemamemory.api.film.FilmDtos.SceneResponse;
import com.cinemamemory.api.security.CurrentUser;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/films")
public class FilmController {
    private final FilmService filmService;

    public FilmController(FilmService filmService) {
        this.filmService = filmService;
    }

    @GetMapping
    List<FilmResponse> listFilms(@CurrentUser Long userId) {
        return filmService.listFilms(userId);
    }

    @PostMapping
    FilmResponse createFilm(@CurrentUser Long userId, @Valid @RequestBody FilmRequest request) {
        return filmService.createFilm(userId, request);
    }

    @PatchMapping("/{filmId}")
    FilmResponse updateFilm(@CurrentUser Long userId, @PathVariable Long filmId, @Valid @RequestBody FilmRequest request) {
        return filmService.updateFilm(userId, filmId, request);
    }

    @DeleteMapping("/{filmId}")
    ResponseEntity<Void> deleteFilm(@CurrentUser Long userId, @PathVariable Long filmId) {
        filmService.deleteFilm(userId, filmId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{filmId}/playback")
    PlaybackResponse playback(@CurrentUser Long userId, @PathVariable Long filmId) {
        return filmService.playback(userId, filmId);
    }

    @GetMapping("/{filmId}/scenes")
    List<SceneResponse> listScenes(@CurrentUser Long userId, @PathVariable Long filmId) {
        return filmService.listScenes(userId, filmId);
    }

    @PostMapping("/{filmId}/scenes")
    SceneResponse createScene(@CurrentUser Long userId, @PathVariable Long filmId, @Valid @RequestBody SceneRequest request) {
        return filmService.createScene(userId, filmId, request);
    }

    @PatchMapping("/{filmId}/scenes/{sceneId}")
    SceneResponse updateScene(
            @CurrentUser Long userId,
            @PathVariable Long filmId,
            @PathVariable Long sceneId,
            @Valid @RequestBody SceneRequest request
    ) {
        return filmService.updateScene(userId, filmId, sceneId, request);
    }

    @DeleteMapping("/{filmId}/scenes/{sceneId}")
    ResponseEntity<Void> deleteScene(@CurrentUser Long userId, @PathVariable Long filmId, @PathVariable Long sceneId) {
        filmService.deleteScene(userId, filmId, sceneId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{filmId}/scenes/order")
    ResponseEntity<Void> reorderScenes(@CurrentUser Long userId, @PathVariable Long filmId, @Valid @RequestBody SceneOrderRequest request) {
        filmService.reorderScenes(userId, filmId, request);
        return ResponseEntity.noContent().build();
    }
}
