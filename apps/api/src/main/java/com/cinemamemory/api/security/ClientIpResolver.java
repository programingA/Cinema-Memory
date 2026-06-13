package com.cinemamemory.api.security;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class ClientIpResolver {
    public String resolve(HttpServletRequest request) {
        String forwardedFor = firstHeaderValue(request.getHeader("X-Forwarded-For"));
        if (StringUtils.hasText(forwardedFor)) {
            return forwardedFor;
        }

        String realIp = firstHeaderValue(request.getHeader("X-Real-IP"));
        if (StringUtils.hasText(realIp)) {
            return realIp;
        }

        return request.getRemoteAddr();
    }

    private String firstHeaderValue(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }

        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .findFirst()
                .orElse(null);
    }
}
