package com.cinemamemory.api.admin;

import com.cinemamemory.api.admin.AdminDtos.AdminFilmResponse;
import com.cinemamemory.api.admin.AdminDtos.AdminSummaryResponse;
import com.cinemamemory.api.admin.AdminDtos.AdminUserResponse;
import com.cinemamemory.api.admin.AdminDtos.UpdateUserRoleRequest;
import com.cinemamemory.api.common.ApiException;
import com.cinemamemory.api.film.FilmRepository;
import com.cinemamemory.api.film.MediaAssetRepository;
import com.cinemamemory.api.film.MemorySceneRepository;
import com.cinemamemory.api.user.User;
import com.cinemamemory.api.user.UserRepository;
import com.cinemamemory.api.user.UserRole;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminService {
    private final UserRepository userRepository;
    private final FilmRepository filmRepository;
    private final MemorySceneRepository sceneRepository;
    private final MediaAssetRepository mediaAssetRepository;

    public AdminService(
            UserRepository userRepository,
            FilmRepository filmRepository,
            MemorySceneRepository sceneRepository,
            MediaAssetRepository mediaAssetRepository
    ) {
        this.userRepository = userRepository;
        this.filmRepository = filmRepository;
        this.sceneRepository = sceneRepository;
        this.mediaAssetRepository = mediaAssetRepository;
    }

    @Transactional(readOnly = true)
    public AdminSummaryResponse summary() {
        return new AdminSummaryResponse(
                userRepository.countByRole(UserRole.USER),
                userRepository.countByRole(UserRole.ADMIN),
                filmRepository.count(),
                sceneRepository.count(),
                mediaAssetRepository.count()
        );
    }

    @Transactional(readOnly = true)
    public List<AdminUserResponse> listUsers() {
        return userRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(AdminUserResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AdminFilmResponse> listFilms() {
        return filmRepository.findAllForAdmin().stream()
                .map(AdminFilmResponse::from)
                .toList();
    }

    @Transactional
    public AdminUserResponse updateUserRole(Long currentAdminId, Long userId, UpdateUserRoleRequest request) {
        if (currentAdminId.equals(userId)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Admins cannot change their own role");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));

        if (user.getRole() == UserRole.ADMIN
                && request.role() != UserRole.ADMIN
                && userRepository.countByRole(UserRole.ADMIN) <= 1) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "At least one admin account is required");
        }

        user.updateRole(request.role());
        return AdminUserResponse.from(user);
    }
}
