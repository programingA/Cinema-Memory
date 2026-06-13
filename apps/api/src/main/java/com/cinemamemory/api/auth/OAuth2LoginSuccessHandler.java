package com.cinemamemory.api.auth;

import com.cinemamemory.api.config.AppProperties;
import com.cinemamemory.api.common.InputSanitizer;
import com.cinemamemory.api.user.User;
import com.cinemamemory.api.user.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

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
    @Transactional
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException, ServletException {
        OAuth2AuthenticationToken token = (OAuth2AuthenticationToken) authentication;
        OAuth2User principal = token.getPrincipal();
        String provider = token.getAuthorizedClientRegistrationId();
        String providerUserId = providerUserId(provider, principal);

        Map<String, Object> attributes = principal.getAttributes();
        User user = oauthAccountRepository.findByProviderAndProviderUserId(provider, providerUserId)
                .map(OAuthAccount::getUser)
                .orElseGet(() -> createAndLinkUser(provider, providerUserId, attributes));
        if (!user.isActive()) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Account is suspended");
            return;
        }

        AuthDtos.TokenResponse tokens = authService.issueTokens(user);
        String redirect = properties.frontendUrl()
                + "/auth/callback?accessToken=" + encode(tokens.accessToken())
                + "&refreshToken=" + encode(tokens.refreshToken());
        response.sendRedirect(redirect);
    }

    private User createAndLinkUser(String provider, String providerUserId, Map<String, Object> attributes) {
        String email = email(provider, providerUserId, attributes).trim().toLowerCase(Locale.ROOT);
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseGet(() -> userRepository.save(new User(
                        email,
                        null,
                        InputSanitizer.requiredPlainText(displayName(provider, email, attributes), "Display name", 120),
                        avatarUrl(provider, attributes)
                )));
        oauthAccountRepository.save(new OAuthAccount(user, provider, providerUserId, email));
        return user;
    }

    private String providerUserId(String provider, OAuth2User principal) {
        if ("kakao".equals(provider)) {
            return attributeAsString(principal.getAttributes(), "id");
        }
        return attributeAsString(principal.getAttributes(), "sub");
    }

    private String email(String provider, String providerUserId, Map<String, Object> attributes) {
        if ("kakao".equals(provider)) {
            Map<?, ?> account = nestedMap(attributes, "kakao_account");
            Object email = account.get("email");
            return email == null ? providerUserId + "@kakao.local" : email.toString();
        }
        return attributeAsString(attributes, "email");
    }

    private String displayName(String provider, String email, Map<String, Object> attributes) {
        if ("kakao".equals(provider)) {
            Map<?, ?> account = nestedMap(attributes, "kakao_account");
            Map<?, ?> profile = nestedMap(account, "profile");
            Object nickname = profile.get("nickname");
            return nickname == null ? "Kakao User" : nickname.toString();
        }
        Object name = attributes.get("name");
        return name == null ? email : name.toString();
    }

    private String avatarUrl(String provider, Map<String, Object> attributes) {
        if ("kakao".equals(provider)) {
            Map<?, ?> account = nestedMap(attributes, "kakao_account");
            Map<?, ?> profile = nestedMap(account, "profile");
            Object avatar = profile.get("profile_image_url");
            return avatar == null ? null : avatar.toString();
        }
        Object picture = attributes.get("picture");
        return picture == null ? null : picture.toString();
    }

    private Map<?, ?> nestedMap(Map<?, ?> attributes, String name) {
        Object value = attributes.get(name);
        return value instanceof Map<?, ?> map ? map : Map.of();
    }

    private String attributeAsString(Map<String, Object> attributes, String name) {
        Object value = attributes.get(name);
        if (value == null) {
            throw new IllegalStateException("OAuth2 user attribute is missing: " + name);
        }
        return value.toString();
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
