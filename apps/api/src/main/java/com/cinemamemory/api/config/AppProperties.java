package com.cinemamemory.api.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(
        String frontendUrl,
        Admin admin,
        Jwt jwt,
        Aws aws
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
            String cloudfrontBaseUrl
    ) {
    }
}
