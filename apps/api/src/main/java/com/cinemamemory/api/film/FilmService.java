package com.cinemamemory.api.film;

import com.cinemamemory.api.common.ApiException;
import com.cinemamemory.api.film.FilmDtos.FilmRequest;
import com.cinemamemory.api.film.FilmDtos.FilmResponse;
import com.cinemamemory.api.film.FilmDtos.PlaybackResponse;
import com.cinemamemory.api.film.FilmDtos.SceneOrderRequest;
import com.cinemamemory.api.film.FilmDtos.SceneRequest;
import com.cinemamemory.api.film.FilmDtos.SceneResponse;
import com.cinemamemory.api.user.User;
import com.cinemamemory.api.user.UserRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FilmService {
    private final FilmRepository filmRepository;
    private final MemorySceneRepository sceneRepository;
    private final UserRepository userRepository;

    public FilmService(FilmRepository filmRepository, MemorySceneRepository sceneRepository, UserRepository userRepository) {
        this.filmRepository = filmRepository;
        this.sceneRepository = sceneRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<FilmResponse> listFilms(Long userId) {
        return filmRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(FilmResponse::from)
                .toList();
    }

    @Transactional
    public FilmResponse createFilm(Long userId, FilmRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));
        Film film = filmRepository.save(new Film(user, request.title(), request.description(), request.coverImageUrl(), request.mood()));
        return FilmResponse.from(film);
    }

    @Transactional
    public FilmResponse updateFilm(Long userId, Long filmId, FilmRequest request) {
        Film film = findFilm(filmId, userId);
        film.update(request.title(), request.description(), request.coverImageUrl(), request.mood());
        return FilmResponse.from(film);
    }

    @Transactional
    public void deleteFilm(Long userId, Long filmId) {
        Film film = findFilm(filmId, userId);
        filmRepository.delete(film);
    }

    @Transactional(readOnly = true)
    public PlaybackResponse playback(Long userId, Long filmId) {
        Film film = filmRepository.findWithScenesByIdAndUserId(filmId, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Film not found"));
        return PlaybackResponse.from(film);
    }

    @Transactional(readOnly = true)
    public List<SceneResponse> listScenes(Long userId, Long filmId) {
        findFilm(filmId, userId);
        return sceneRepository.findByFilmIdOrderBySortOrderAsc(filmId).stream()
                .map(SceneResponse::from)
                .toList();
    }

    @Transactional
    public SceneResponse createScene(Long userId, Long filmId, SceneRequest request) {
        Film film = findFilm(filmId, userId);
        MemoryScene scene = sceneRepository.save(new MemoryScene(
                film,
                request.title(),
                request.body(),
                request.memoryDate(),
                request.location(),
                request.mood(),
                request.sortOrder()
        ));
        return SceneResponse.from(scene);
    }

    @Transactional
    public SceneResponse updateScene(Long userId, Long filmId, Long sceneId, SceneRequest request) {
        MemoryScene scene = sceneRepository.findByIdAndFilmUserId(sceneId, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Scene not found"));
        ensureSceneBelongsToFilm(scene, filmId);
        scene.update(request.title(), request.body(), request.memoryDate(), request.location(), request.mood(), request.sortOrder());
        return SceneResponse.from(scene);
    }

    @Transactional
    public void deleteScene(Long userId, Long filmId, Long sceneId) {
        MemoryScene scene = sceneRepository.findByIdAndFilmUserId(sceneId, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Scene not found"));
        ensureSceneBelongsToFilm(scene, filmId);
        sceneRepository.delete(scene);
    }

    @Transactional
    public void reorderScenes(Long userId, Long filmId, SceneOrderRequest request) {
        findFilm(filmId, userId);
        for (FilmDtos.SceneOrderItem item : request.scenes()) {
            MemoryScene scene = sceneRepository.findByIdAndFilmUserId(item.sceneId(), userId)
                    .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Scene not found"));
            ensureSceneBelongsToFilm(scene, filmId);
            scene.updateSortOrder(item.sortOrder());
        }
    }

    private Film findFilm(Long filmId, Long userId) {
        return filmRepository.findByIdAndUserId(filmId, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Film not found"));
    }

    private void ensureSceneBelongsToFilm(MemoryScene scene, Long filmId) {
        if (!scene.getFilm().getId().equals(filmId)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Scene does not belong to the film");
        }
    }
}
