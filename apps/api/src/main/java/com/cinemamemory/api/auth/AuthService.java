package com.cinemamemory.api.auth;

import com.cinemamemory.api.auth.AuthDtos.LoginRequest;
import com.cinemamemory.api.auth.AuthDtos.MeResponse;
import com.cinemamemory.api.auth.AuthDtos.SignupRequest;
import com.cinemamemory.api.auth.AuthDtos.TokenResponse;
import com.cinemamemory.api.common.ApiException;
import com.cinemamemory.api.common.InputSanitizer;
import com.cinemamemory.api.security.JwtService;
import com.cinemamemory.api.user.User;
import com.cinemamemory.api.user.UserRepository;
import com.cinemamemory.api.user.UserRole;
import java.util.Locale;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            RefreshTokenService refreshTokenService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
    }

    @Transactional
    public TokenResponse signup(SignupRequest request) {
        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw signupFailed();
        }

        try {
            User user = userRepository.save(new User(
                    email,
                    passwordEncoder.encode(request.password()),
                    InputSanitizer.requiredPlainText(request.displayName(), "Display name", 120),
                    null
            ));
            return issueTokens(user);
        } catch (DataIntegrityViolationException exception) {
            throw signupFailed();
        }
    }

    @Transactional
    public TokenResponse login(LoginRequest request) {
        User user = userRepository.findByEmailIgnoreCase(normalizeEmail(request.email()))
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));
        if (user.getPasswordHash() == null || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }
        ensureActive(user, HttpStatus.FORBIDDEN);
        return issueTokens(user);
    }

    @Transactional
    public TokenResponse refresh(String refreshToken) {
        Long userId = refreshTokenService.resolve(refreshToken)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Invalid refresh token"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Invalid refresh token"));
        ensureActive(user, HttpStatus.UNAUTHORIZED);

        refreshTokenService.revoke(refreshToken);
        return issueTokens(user);
    }

    public void logout(String refreshToken) {
        refreshTokenService.revoke(refreshToken);
    }

    @Transactional
    public MeResponse me(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));
        ensureActive(user, HttpStatus.UNAUTHORIZED);
        return new MeResponse(
                user.getEmail(),
                user.getDisplayName(),
                user.getAvatarUrl(),
                user.getRole().name(),
                user.getStatus().name(),
                user.getRole() == UserRole.ADMIN
        );
    }

    public TokenResponse issueTokens(User user) {
        String accessToken = jwtService.issueAccessToken(user);
        String refreshToken = refreshTokenService.issue(user.getId());
        return TokenResponse.bearer(accessToken, refreshToken);
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private ApiException signupFailed() {
        return new ApiException(HttpStatus.BAD_REQUEST, "Signup could not be completed");
    }

    private void ensureActive(User user, HttpStatus status) {
        if (!user.isActive()) {
            throw new ApiException(status, "Account is suspended");
        }
    }
}
