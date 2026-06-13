package com.cinemamemory.api.admin;

import com.cinemamemory.api.admin.AdminDtos.AdminFilmResponse;
import com.cinemamemory.api.admin.AdminDtos.AdminSummaryResponse;
import com.cinemamemory.api.admin.AdminDtos.AdminUserResponse;
import com.cinemamemory.api.admin.AdminDtos.UpdateUserRoleRequest;
import com.cinemamemory.api.admin.AdminDtos.UpdateUserStatusRequest;
import com.cinemamemory.api.auth.OAuthAccountRepository;
import com.cinemamemory.api.auth.RefreshTokenService;
import com.cinemamemory.api.common.ApiException;
import com.cinemamemory.api.film.FilmRepository;
import com.cinemamemory.api.film.MediaAssetRepository;
import com.cinemamemory.api.film.MemorySceneRepository;
import com.cinemamemory.api.film.TagRepository;
import com.cinemamemory.api.user.User;
import com.cinemamemory.api.user.UserRepository;
import com.cinemamemory.api.user.UserRole;
import com.cinemamemory.api.user.UserStatus;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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
    private final TagRepository tagRepository;
    private final OAuthAccountRepository oauthAccountRepository;
    private final RefreshTokenService refreshTokenService;

    public AdminService(
            UserRepository userRepository,
            FilmRepository filmRepository,
            MemorySceneRepository sceneRepository,
            MediaAssetRepository mediaAssetRepository,
            TagRepository tagRepository,
            OAuthAccountRepository oauthAccountRepository,
            RefreshTokenService refreshTokenService
    ) {
        this.userRepository = userRepository;
        this.filmRepository = filmRepository;
        this.sceneRepository = sceneRepository;
        this.mediaAssetRepository = mediaAssetRepository;
        this.tagRepository = tagRepository;
        this.oauthAccountRepository = oauthAccountRepository;
        this.refreshTokenService = refreshTokenService;
    }

    @Transactional(readOnly = true)
    public AdminSummaryResponse summary() {
        return new AdminSummaryResponse(
                userRepository.countByRole(UserRole.USER),
                userRepository.countByRole(UserRole.ADMIN),
                userRepository.countByStatus(UserStatus.SUSPENDED),
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
                && user.isActive()
                && activeAdminCount() <= 1) {
            throw lastActiveAdminRequired();
        }

        user.updateRole(request.role());
        return AdminUserResponse.from(user);
    }

    @Transactional
    public AdminUserResponse updateUserStatus(Long currentAdminId, Long userId, UpdateUserStatusRequest request) {
        if (currentAdminId.equals(userId)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Admins cannot change their own status");
        }

        User user = findUser(userId);
        if (user.getStatus() == request.status()) {
            return AdminUserResponse.from(user);
        }

        if (user.getRole() == UserRole.ADMIN
                && user.isActive()
                && request.status() != UserStatus.ACTIVE
                && activeAdminCount() <= 1) {
            throw lastActiveAdminRequired();
        }

        user.updateStatus(request.status());
        if (request.status() != UserStatus.ACTIVE) {
            refreshTokenService.revokeAllForUser(userId);
        }

        return AdminUserResponse.from(user);
    }

    @Transactional
    public void deleteUser(Long currentAdminId, Long userId) {
        if (currentAdminId.equals(userId)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Admins cannot delete their own account");
        }

        User user = findUser(userId);
        if (user.getRole() == UserRole.ADMIN && user.isActive() && activeAdminCount() <= 1) {
            throw lastActiveAdminRequired();
        }

        List<String> localMediaUrls = mediaAssetRepository.findCdnUrlsByUserId(userId);
        refreshTokenService.revokeAllForUser(userId);
        filmRepository.deleteFilmTagsByUserId(userId);
        mediaAssetRepository.deleteByUserId(userId);
        sceneRepository.deleteByFilmUserId(userId);
        filmRepository.deleteByUserId(userId);
        oauthAccountRepository.deleteByUserId(userId);
        tagRepository.deleteByUserId(userId);
        userRepository.delete(user);
        deleteLocalMediaFiles(localMediaUrls);
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private long activeAdminCount() {
        return userRepository.countByRoleAndStatus(UserRole.ADMIN, UserStatus.ACTIVE);
    }

    private ApiException lastActiveAdminRequired() {
        return new ApiException(HttpStatus.BAD_REQUEST, "At least one active admin account is required");
    }

    private void deleteLocalMediaFiles(List<String> mediaUrls) {
        Path uploadDir = Path.of("uploads", "media").toAbsolutePath().normalize();
        for (String mediaUrl : mediaUrls) {
            if (mediaUrl == null || !mediaUrl.startsWith("/uploads/media/")) {
                continue;
            }

            Path target = uploadDir.resolve(mediaUrl.substring("/uploads/media/".length())).normalize();
            if (!target.startsWith(uploadDir)) {
                continue;
            }

            try {
                Files.deleteIfExists(target);
            } catch (IOException ignored) {
                // DB deletion is authoritative; stale local files can be cleaned later.
            }
        }
    }
}
