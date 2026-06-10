package com.cinemamemory.api.film;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface FilmRepository extends JpaRepository<Film, Long> {
    List<Film> findByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<Film> findByIdAndUserId(Long id, Long userId);

    @EntityGraph(attributePaths = {"user", "scenes"})
    @Query("select distinct f from Film f order by f.createdAt desc")
    List<Film> findAllForAdmin();

    @EntityGraph(attributePaths = {"scenes"})
    @Query("select f from Film f where f.id = :id and f.user.id = :userId")
    Optional<Film> findWithScenesByIdAndUserId(Long id, Long userId);
}
