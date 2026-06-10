package com.cinemamemory.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class CinemaMemoryApiApplication {
    public static void main(String[] args) {
        SpringApplication.run(CinemaMemoryApiApplication.class, args);
    }
}
