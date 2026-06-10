package com.cinemamemory.api.config;

import java.nio.file.Path;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class LocalMediaConfig implements WebMvcConfigurer {
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String mediaLocation = Path.of("uploads", "media").toAbsolutePath().normalize().toUri().toString();
        registry.addResourceHandler("/uploads/media/**")
                .addResourceLocations(mediaLocation);
    }
}
