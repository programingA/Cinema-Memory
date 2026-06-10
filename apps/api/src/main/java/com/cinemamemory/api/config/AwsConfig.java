package com.cinemamemory.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Configuration
public class AwsConfig {
    @Bean
    S3Presigner s3Presigner(AppProperties properties) {
        return S3Presigner.builder()
                .region(Region.of(properties.aws().region()))
                .build();
    }
}
