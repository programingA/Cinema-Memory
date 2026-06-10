package com.cinemamemory.api.config;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cinemamemory.api.auth.AuthController;
import com.cinemamemory.api.auth.AuthDtos.SignupRequest;
import com.cinemamemory.api.auth.AuthDtos.TokenResponse;
import com.cinemamemory.api.auth.AuthService;
import com.cinemamemory.api.auth.OAuth2LoginSuccessHandler;
import com.cinemamemory.api.auth.OAuth2UserProvisionService;
import com.cinemamemory.api.debug.BuildInfoController;
import com.cinemamemory.api.debug.BuildInfoFilter;
import com.cinemamemory.api.security.JwtAuthenticationFilter;
import com.cinemamemory.api.security.JwtService;
import com.cinemamemory.api.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest({AuthController.class, BuildInfoController.class})
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, BuildInfoFilter.class})
class SecurityConfigTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private OAuth2UserProvisionService oauth2UserProvisionService;

    @MockitoBean
    private OAuth2LoginSuccessHandler oauth2LoginSuccessHandler;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @MockitoBean
    private ClientRegistrationRepository clientRegistrationRepository;

    @MockitoBean
    private OAuth2AuthorizedClientRepository oAuth2AuthorizedClientRepository;

    @Test
    void signupIsPublicJsonApi() throws Exception {
        given(authService.signup(any(SignupRequest.class))).willReturn(TokenResponse.bearer("access", "refresh"));

        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"test1234@example.com","password":"12345678","displayName":"test"}
                                """))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist("Location"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"));
    }

    @Test
    void debugVersionIsPublicJsonApi() throws Exception {
        mockMvc.perform(get("/debug/version").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist("Location"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.application").value("cinema-memory-api"));
    }

    @Test
    void protectedApiReturnsJsonUnauthorizedInsteadOfLoginRedirect() throws Exception {
        mockMvc.perform(get("/auth/me").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(header().doesNotExist("Location"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(401));
    }
}
