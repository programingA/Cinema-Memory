package com.cinemamemory.api.auth;

import com.cinemamemory.api.auth.AuthDtos.LoginRequest;
import com.cinemamemory.api.auth.AuthDtos.LogoutRequest;
import com.cinemamemory.api.auth.AuthDtos.MeResponse;
import com.cinemamemory.api.auth.AuthDtos.RefreshRequest;
import com.cinemamemory.api.auth.AuthDtos.SignupRequest;
import com.cinemamemory.api.auth.AuthDtos.TokenResponse;
import com.cinemamemory.api.security.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/signup")
    TokenResponse signup(@Valid @RequestBody SignupRequest request) {
        return authService.signup(request);
    }

    @PostMapping("/login")
    TokenResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/refresh")
    TokenResponse refresh(@Valid @RequestBody RefreshRequest request) {
        return authService.refresh(request.refreshToken());
    }

    @PostMapping("/logout")
    ResponseEntity<Void> logout(@Valid @RequestBody LogoutRequest request) {
        authService.logout(request.refreshToken());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    MeResponse me(@CurrentUser Long userId) {
        return authService.me(userId);
    }
}
