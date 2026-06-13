package com.cinemamemory.api.film;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface TagRepository extends JpaRepository<Tag, Long> {
    @Modifying
    @Query("delete from Tag tag where tag.user.id = :userId")
    int deleteByUserId(Long userId);
}
