package com.cinemamemory.api.film;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface MemorySceneRepository extends JpaRepository<MemoryScene, Long> {
    List<MemoryScene> findByFilmIdOrderBySortOrderAsc(Long filmId);

    Optional<MemoryScene> findByIdAndFilmUserId(Long id, Long userId);

    @Modifying
    @Query("delete from MemoryScene scene where scene.film.user.id = :userId")
    int deleteByFilmUserId(Long userId);
}
