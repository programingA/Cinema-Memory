package com.cinemamemory.api.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class AuthDtos {
    private AuthDtos() {
    }

    public record SignupRequest(
            @Email @NotBlank String email,
            @Size(min = 8, message = "Password must be at least 8 characters") String password,
            @NotBlank String displayName
    ) {
    }

    public record LoginRequest(
            @Email @NotBlank String email,
            @NotBlank String password
    ) {
    }

    public record RefreshRequest(@NotBlank String refreshToken) {
    }

    public record LogoutRequest(@NotBlank String refreshToken) {
    }

    public record TokenResponse(String accessToken, String refreshToken, String tokenType) {
        public static TokenResponse bearer(String accessToken, String refreshToken) {
            return new TokenResponse(accessToken, refreshToken, "Bearer");
        }
    }

    public record MeResponse(String email, String displayName, String avatarUrl, String role, String status, boolean admin) {
    }
}
