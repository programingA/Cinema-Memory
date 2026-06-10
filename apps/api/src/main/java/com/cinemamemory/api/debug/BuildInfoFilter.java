package com.cinemamemory.api.debug;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class BuildInfoFilter extends OncePerRequestFilter {
    private static final String VERSION_PATH = "/debug/version";

    private final ObjectMapper objectMapper;
    private final String imageRevision;
    private final String buildTime;

    public BuildInfoFilter(
            ObjectMapper objectMapper,
            @Value("${IMAGE_REVISION:unknown}") String imageRevision,
            @Value("${BUILD_TIME:unknown}") String buildTime
    ) {
        this.objectMapper = objectMapper;
        this.imageRevision = imageRevision;
        this.buildTime = buildTime;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !HttpMethod.GET.name().equalsIgnoreCase(request.getMethod()) || !VERSION_PATH.equals(requestPath(request));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), Map.of(
                "application", "cinema-memory-api",
                "imageRevision", imageRevision,
                "buildTime", buildTime,
                "serverTime", Instant.now().toString()
        ));
    }

    private String requestPath(HttpServletRequest request) {
        String path = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (!contextPath.isBlank() && path.startsWith(contextPath)) {
            return path.substring(contextPath.length());
        }
        return path;
    }
}
