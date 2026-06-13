package com.cinemamemory.api.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class SecurityHeadersFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("X-Frame-Options", "DENY");
        response.setHeader("Referrer-Policy", "no-referrer");
        response.setHeader("Permissions-Policy", "camera=(), microphone=(), geolocation=()");
        response.setHeader("Content-Security-Policy", "default-src 'none'; frame-ancestors 'none'; base-uri 'none'; form-action 'none'");

        if (isHttps(request)) {
            response.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains; preload");
        }

        filterChain.doFilter(request, response);
    }

    private boolean isHttps(HttpServletRequest request) {
        return request.isSecure()
                || "https".equalsIgnoreCase(request.getHeader("X-Forwarded-Proto"))
                || String.valueOf(request.getHeader("Forwarded")).toLowerCase().contains("proto=https");
    }
}
