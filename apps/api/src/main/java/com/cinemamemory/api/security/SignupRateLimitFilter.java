package com.cinemamemory.api.security;

import com.cinemamemory.api.config.AppProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@ConditionalOnBean(StringRedisTemplate.class)
public class SignupRateLimitFilter extends OncePerRequestFilter {
    private static final Duration WINDOW = Duration.ofMinutes(1);
    private static final String SIGNUP_PATH = "/auth/signup";

    private final StringRedisTemplate redisTemplate;
    private final ClientIpResolver clientIpResolver;
    private final AppProperties properties;

    public SignupRateLimitFilter(
            StringRedisTemplate redisTemplate,
            ClientIpResolver clientIpResolver,
            AppProperties properties
    ) {
        this.redisTemplate = redisTemplate;
        this.clientIpResolver = clientIpResolver;
        this.properties = properties;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !HttpMethod.POST.matches(request.getMethod()) || !SIGNUP_PATH.equals(request.getServletPath());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        int threshold = signupRateLimitThreshold();
        if (threshold <= 0) {
            filterChain.doFilter(request, response);
            return;
        }

        String key = "rate:signup:" + hash(clientIpResolver.resolve(request));
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1L) {
            redisTemplate.expire(key, WINDOW);
        }

        if (count != null && count >= threshold) {
            response.setStatus(429);
            response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(WINDOW.toSeconds()));
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.getWriter().write("""
                    {"status":429,"error":"Too Many Requests","message":"Too many signup attempts. Please retry later."}
                    """);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private int signupRateLimitThreshold() {
        AppProperties.Security security = properties.security();
        return security == null ? 5 : security.signupRateLimitPerMinute();
    }

    private String hash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }
}
