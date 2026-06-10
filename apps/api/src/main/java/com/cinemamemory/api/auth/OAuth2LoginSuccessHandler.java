package com.cinemamemory.api.auth;

import com.cinemamemory.api.config.AppProperties;
import com.cinemamemory.api.user.User;
import com.cinemamemory.api.user.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

@Component
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {
    private final OAuthAccountRepository oauthAccountRepository;
    private final UserRepository userRepository;
    private final AuthService authService;
    private final AppProperties properties;

    public OAuth2LoginSuccessHandler(
            OAuthAccountRepository oauthAccountRepository,
            UserRepository userRepository,
            AuthService authService,
            AppProperties properties
    ) {
        this.oauthAccountRepository = oauthAccountRepository;
        this.userRepository = userRepository;
        this.authService = authService;
        this.properties = properties;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException, ServletException {
        OAuth2AuthenticationToken token = (OAuth2AuthenticationToken) authentication;
        OAuth2User principal = token.getPrincipal();
        String provider = token.getAuthorizedClientRegistrationId();
        String providerUserId = providerUserId(provider, principal);

        User user = oauthAccountRepository.findByProviderAndProviderUserId(provider, providerUserId)
                .map(OAuthAccount::getUser)
                .orElseGet(() -> userRepository.findByEmail(String.valueOf(principal.getAttribute("email"))).orElseThrow());

        AuthDtos.TokenResponse tokens = authService.issueTokens(user);
        String redirect = properties.frontendUrl()
                + "/auth/callback?accessToken=" + encode(tokens.accessToken())
                + "&refreshToken=" + encode(tokens.refreshToken());
        response.sendRedirect(redirect);
    }

    private String providerUserId(String provider, OAuth2User principal) {
        if ("kakao".equals(provider)) {
            return String.valueOf(principal.getAttribute("id"));
        }
        return String.valueOf(principal.getAttribute("sub"));
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
