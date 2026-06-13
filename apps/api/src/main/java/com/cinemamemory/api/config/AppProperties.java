package com.cinemamemory.api.config;

import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(
        String frontendUrl,
        Admin admin,
        Jwt jwt,
        Aws aws,
        Security security,
        Media media
) {
    public record Admin(
            String email,
            String password,
            String displayName
    ) {
    }

    public record Jwt(
            String issuer,
            String secret,
            long accessTokenMinutes,
            long refreshTokenDays
    ) {
        public Duration accessTokenTtl() {
            return Duration.ofMinutes(accessTokenMinutes);
        }

        public Duration refreshTokenTtl() {
            return Duration.ofDays(refreshTokenDays);
        }
    }

    public record Aws(
            String region,
            String s3Bucket,
            String s3Prefix,
            String cloudfrontBaseUrl
    ) {
    }

    public record Security(
            List<String> corsAllowedOrigins,
            boolean requireHttps,
            int signupRateLimitPerMinute
    ) {
    }

    public record Media(
            long maxUploadBytes,
            String storageMode
    ) {
        public long effectiveMaxUploadBytes() {
            return maxUploadBytes > 0 ? maxUploadBytes : 50 * 1024 * 1024;
        }

        public boolean usesLocalStorage() {
            return "local".equalsIgnoreCase(storageMode);
        }
    }
}
