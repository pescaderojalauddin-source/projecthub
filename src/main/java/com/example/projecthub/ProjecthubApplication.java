package com.example.projecthub;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// точка входа Spring Boot
// порт 8080, конфиги в application.properties
@SpringBootApplication
public class ProjecthubApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProjecthubApplication.class, args);
    }
}
