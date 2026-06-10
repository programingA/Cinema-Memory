package com.cinemamemory.api.debug;

import java.time.Instant;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/debug")
public class BuildInfoController {
    private final String imageRevision;
    private final String buildTime;

    public BuildInfoController(
            @Value("${IMAGE_REVISION:unknown}") String imageRevision,
            @Value("${BUILD_TIME:unknown}") String buildTime
    ) {
        this.imageRevision = imageRevision;
        this.buildTime = buildTime;
    }

    @GetMapping("/version")
    Map<String, String> version() {
        return Map.of(
                "application", "cinema-memory-api",
                "imageRevision", imageRevision,
                "buildTime", buildTime,
                "serverTime", Instant.now().toString()
        );
    }
}
