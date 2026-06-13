package com.cinemamemory.api.auth;

import com.cinemamemory.api.common.InputSanitizer;
import com.cinemamemory.api.user.User;
import com.cinemamemory.api.user.UserRepository;
import java.util.Locale;
import java.util.Map;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OAuth2UserProvisionService extends DefaultOAuth2UserService {
    private final UserRepository userRepository;
    private final OAuthAccountRepository oauthAccountRepository;

    public OAuth2UserProvisionService(
            UserRepository userRepository,
            OAuthAccountRepository oauthAccountRepository
    ) {
        this.userRepository = userRepository;
        this.oauthAccountRepository = oauthAccountRepository;
    }

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oauth2User = super.loadUser(userRequest);
        String provider = userRequest.getClientRegistration().getRegistrationId();
        OAuthProfile profile = OAuthProfile.from(provider, oauth2User.getAttributes());

        oauthAccountRepository.findByProviderAndProviderUserId(provider, profile.providerUserId())
                .orElseGet(() -> {
                    String email = profile.email().trim().toLowerCase(Locale.ROOT);
                    User user = userRepository.findByEmailIgnoreCase(email)
                            .orElseGet(() -> userRepository.save(new User(
                                    email,
                                    null,
                                    InputSanitizer.requiredPlainText(profile.displayName(), "Display name", 120),
                                    profile.avatarUrl()
                            )));
                    return oauthAccountRepository.save(new OAuthAccount(user, provider, profile.providerUserId(), email));
                });

        return oauth2User;
    }

    private record OAuthProfile(String providerUserId, String email, String displayName, String avatarUrl) {
        static OAuthProfile from(String provider, Map<String, Object> attributes) {
            if ("kakao".equals(provider)) {
                Map<?, ?> account = (Map<?, ?>) attributes.getOrDefault("kakao_account", Map.of());
                Object profileValue = account.get("profile");
                Map<?, ?> profile = profileValue instanceof Map<?, ?> profileMap ? profileMap : Map.of();
                String email = account.get("email") == null
                        ? attributes.get("id") + "@kakao.local"
                        : String.valueOf(account.get("email"));
                String nickname = profile.get("nickname") == null ? "Kakao User" : String.valueOf(profile.get("nickname"));
                String avatar = profile.get("profile_image_url") == null ? null : String.valueOf(profile.get("profile_image_url"));
                return new OAuthProfile(String.valueOf(attributes.get("id")), email, nickname, avatar);
            }

            String email = String.valueOf(attributes.get("email"));
            String name = String.valueOf(attributes.getOrDefault("name", email));
            String avatar = attributes.get("picture") == null ? null : String.valueOf(attributes.get("picture"));
            return new OAuthProfile(String.valueOf(attributes.get("sub")), email, name, avatar);
        }
    }
}
