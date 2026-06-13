package com.cinemamemory.api.auth;

import com.cinemamemory.api.config.AppProperties;
import java.time.Duration;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class RefreshTokenService {
    private static final String PREFIX = "refresh:";

    private final StringRedisTemplate redisTemplate;
    private final Duration ttl;

    public RefreshTokenService(StringRedisTemplate redisTemplate, AppProperties properties) {
        this.redisTemplate = redisTemplate;
        this.ttl = properties.jwt().refreshTokenTtl();
    }

    public String issue(Long userId) {
        String token = UUID.randomUUID().toString();
        redisTemplate.opsForValue().set(PREFIX + token, String.valueOf(userId), ttl);
        return token;
    }

    public Optional<Long> resolve(String token) {
        String userId = redisTemplate.opsForValue().get(PREFIX + token);
        return userId == null ? Optional.empty() : Optional.of(Long.valueOf(userId));
    }

    public void revoke(String token) {
        redisTemplate.delete(PREFIX + token);
    }

    public void revokeAllForUser(Long userId) {
        Set<String> keys = redisTemplate.keys(PREFIX + "*");
        if (keys == null || keys.isEmpty()) {
            return;
        }

        String expectedUserId = String.valueOf(userId);
        keys.stream()
                .filter(key -> expectedUserId.equals(redisTemplate.opsForValue().get(key)))
                .forEach(redisTemplate::delete);
    }
}
