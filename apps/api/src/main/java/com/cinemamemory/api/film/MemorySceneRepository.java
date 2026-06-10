package com.cinemamemory.api.film;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemorySceneRepository extends JpaRepository<MemoryScene, Long> {
    List<MemoryScene> findByFilmIdOrderBySortOrderAsc(Long filmId);

    Optional<MemoryScene> findByIdAndFilmUserId(Long id, Long userId);
}
