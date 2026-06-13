package com.cinemamemory.api.config;

import com.cinemamemory.api.auth.OAuth2LoginSuccessHandler;
import com.cinemamemory.api.auth.OAuth2UserProvisionService;
import com.cinemamemory.api.security.JwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletResponse;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.core.annotation.Order;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.CorsUtils;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.util.StringUtils;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@EnableConfigurationProperties(AppProperties.class)
public class SecurityConfig {
    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);
    private static final List<String> PUBLIC_ENDPOINTS = List.of(
            "/uploads/media/**",
            "POST /auth/signup",
            "POST /auth/login",
            "POST /auth/refresh",
            "GET /actuator/health",
            "GET /debug/version"
    );
    private static final RequestMatcher PUBLIC_API_REQUESTS = new OrRequestMatcher(
            new AntPathRequestMatcher("/uploads/media/**"),
            new AntPathRequestMatcher("/auth/signup", HttpMethod.POST.name()),
            new AntPathRequestMatcher("/auth/login", HttpMethod.POST.name()),
            new AntPathRequestMatcher("/auth/refresh", HttpMethod.POST.name()),
            new AntPathRequestMatcher("/actuator/health", HttpMethod.GET.name()),
            new AntPathRequestMatcher("/debug/version", HttpMethod.GET.name())
    );

    private final AppProperties properties;

    public SecurityConfig(AppProperties properties) {
        this.properties = properties;
    }

    @Bean
    @Order(1)
    SecurityFilterChain publicApiSecurityFilterChain(HttpSecurity http) throws Exception {
        log.info("Configuring public API security chain. Public endpoints: {}", PUBLIC_ENDPOINTS);

        http
                .securityMatcher(PUBLIC_API_REQUESTS)
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .requestCache(cache -> cache.disable())
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());

        return http.build();
    }

    @Bean
    @Order(2)
    SecurityFilterChain applicationSecurityFilterChain(
            HttpSecurity http,
            JwtAuthenticationFilter jwtAuthenticationFilter,
            OAuth2UserProvisionService oauth2UserProvisionService,
            OAuth2LoginSuccessHandler oauth2LoginSuccessHandler
    ) throws Exception {
        log.info("Configuring authenticated API security chain");

        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .requestCache(cache -> cache.disable())
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .exceptionHandling(exception -> exception.authenticationEntryPoint((request, response, authException) -> {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    response.setCharacterEncoding("UTF-8");
                    response.getWriter().write("""
                            {"status":401,"error":"Unauthorized","message":"Authentication is required"}
                            """);
                }))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(CorsUtils::isPreFlightRequest).permitAll()
                        .requestMatchers(PUBLIC_API_REQUESTS).permitAll()
                        .requestMatchers("/oauth2/**", "/login/oauth2/**").permitAll()
                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").hasRole("ADMIN")
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .oauth2Login(oauth -> oauth
                        .userInfoEndpoint(userInfo -> userInfo.userService(oauth2UserProvisionService))
                        .successHandler(oauth2LoginSuccessHandler))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    AuthenticationProvider authenticationProvider(UserDetailsService userDetailsService, PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }

    @Bean
    AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(allowedOrigins());
        configuration.setAllowedMethods(List.of("GET", "POST", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept", "Origin", "X-Requested-With"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    private List<String> allowedOrigins() {
        Set<String> origins = new LinkedHashSet<>();
        AppProperties.Security security = properties.security();
        if (security != null && security.corsAllowedOrigins() != null) {
            security.corsAllowedOrigins().stream()
                    .filter(StringUtils::hasText)
                    .map(String::trim)
                    .map(origin -> origin.replaceAll("/$", ""))
                    .forEach(origins::add);
        }

        if (StringUtils.hasText(properties.frontendUrl())) {
            origins.add(properties.frontendUrl().trim().replaceAll("/$", ""));
        }

        return List.copyOf(origins);
    }
}
