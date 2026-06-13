package com.cinemamemory.api.admin;

import com.cinemamemory.api.admin.AdminDtos.AdminFilmResponse;
import com.cinemamemory.api.admin.AdminDtos.AdminSummaryResponse;
import com.cinemamemory.api.admin.AdminDtos.AdminUserResponse;
import com.cinemamemory.api.admin.AdminDtos.UpdateUserRoleRequest;
import com.cinemamemory.api.admin.AdminDtos.UpdateUserStatusRequest;
import com.cinemamemory.api.media.MediaDtos.S3MigrationResponse;
import com.cinemamemory.api.media.MediaService;
import com.cinemamemory.api.security.CurrentUser;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/admin")
public class AdminController {
    private final AdminService adminService;
    private final MediaService mediaService;

    public AdminController(AdminService adminService, MediaService mediaService) {
        this.adminService = adminService;
        this.mediaService = mediaService;
    }

    @GetMapping("/summary")
    AdminSummaryResponse summary() {
        return adminService.summary();
    }

    @GetMapping("/users")
    List<AdminUserResponse> listUsers() {
        return adminService.listUsers();
    }

    @GetMapping("/films")
    List<AdminFilmResponse> listFilms() {
        return adminService.listFilms();
    }

    @PatchMapping("/users/{userId}/role")
    AdminUserResponse updateUserRole(
            @CurrentUser Long currentAdminId,
            @PathVariable Long userId,
            @Valid @RequestBody UpdateUserRoleRequest request
    ) {
        return adminService.updateUserRole(currentAdminId, userId, request);
    }

    @PatchMapping("/users/{userId}/status")
    AdminUserResponse updateUserStatus(
            @CurrentUser Long currentAdminId,
            @PathVariable Long userId,
            @Valid @RequestBody UpdateUserStatusRequest request
    ) {
        return adminService.updateUserStatus(currentAdminId, userId, request);
    }

    @DeleteMapping("/users/{userId}")
    ResponseEntity<Void> deleteUser(@CurrentUser Long currentAdminId, @PathVariable Long userId) {
        adminService.deleteUser(currentAdminId, userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/media/backfill", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ResponseEntity<Void> backfillLocalMedia(
            @RequestParam String cdnUrl,
            @RequestParam MultipartFile file
    ) {
        mediaService.backfillLocalMedia(cdnUrl, file);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/media/migrate-s3")
    S3MigrationResponse migrateLegacyMediaToS3(
            @RequestParam(defaultValue = "100") int limit,
            @RequestParam(defaultValue = "false") boolean clearLegacyData
    ) {
        return mediaService.migrateLegacyMediaToS3(limit, clearLegacyData);
    }
}
