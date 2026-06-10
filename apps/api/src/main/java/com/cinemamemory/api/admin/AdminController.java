package com.cinemamemory.api.admin;

import com.cinemamemory.api.admin.AdminDtos.AdminFilmResponse;
import com.cinemamemory.api.admin.AdminDtos.AdminSummaryResponse;
import com.cinemamemory.api.admin.AdminDtos.AdminUserResponse;
import com.cinemamemory.api.admin.AdminDtos.UpdateUserRoleRequest;
import com.cinemamemory.api.security.CurrentUser;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin")
public class AdminController {
    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
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
}
