package com.cinemamemory.api.admin;

import com.cinemamemory.api.film.Film;
import com.cinemamemory.api.user.User;
import com.cinemamemory.api.user.UserRole;
import com.cinemamemory.api.user.UserStatus;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

public final class AdminDtos {
    private AdminDtos() {
    }

    public record AdminSummaryResponse(
            long userCount,
            long adminCount,
            long suspendedCount,
            long filmCount,
            long sceneCount,
            long mediaCount
    ) {
    }

    public record AdminUserResponse(
            Long id,
            String email,
            String displayName,
            String avatarUrl,
            String role,
            String status,
            Instant createdAt
    ) {
        static AdminUserResponse from(User user) {
            return new AdminUserResponse(
                    user.getId(),
                    user.getEmail(),
                    user.getDisplayName(),
                    user.getAvatarUrl(),
                    user.getRole().name(),
                    user.getStatus().name(),
                    user.getCreatedAt()
            );
        }
    }

    public record AdminFilmResponse(
            Long id,
            String title,
            String ownerEmail,
            String ownerDisplayName,
            String visibility,
            Instant createdAt,
            int sceneCount
    ) {
        static AdminFilmResponse from(Film film) {
            return new AdminFilmResponse(
                    film.getId(),
                    film.getTitle(),
                    film.getUser().getEmail(),
                    film.getUser().getDisplayName(),
                    film.getVisibility().name(),
                    film.getCreatedAt(),
                    film.getScenes().size()
            );
        }
    }

    public record UpdateUserRoleRequest(@NotNull UserRole role) {
    }

    public record UpdateUserStatusRequest(@NotNull UserStatus status) {
    }
}
