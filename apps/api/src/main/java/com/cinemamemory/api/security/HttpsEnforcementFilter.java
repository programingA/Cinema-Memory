package com.cinemamemory.api.security;

import com.cinemamemory.api.config.AppProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class HttpsEnforcementFilter extends OncePerRequestFilter {
    private final AppProperties properties;

    public HttpsEnforcementFilter(AppProperties properties) {
        this.properties = properties;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (requiresHttps() && !isHttps(request)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.getWriter().write("""
                    {"status":403,"error":"Forbidden","message":"HTTPS is required"}
                    """);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean requiresHttps() {
        AppProperties.Security security = properties.security();
        return security != null && security.requireHttps();
    }

    private boolean isHttps(HttpServletRequest request) {
        return request.isSecure()
                || "https".equalsIgnoreCase(request.getHeader("X-Forwarded-Proto"))
                || String.valueOf(request.getHeader("Forwarded")).toLowerCase().contains("proto=https");
    }
}
