package com.example.projecthub;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Точка входа в приложение ProjectHub.
 *
 * <p>Spring Boot 3 приложение, реализующее систему управления проектами и задачами:
 * проекты, задачи внутри проектов, комментарии к задачам, разграничение прав по ролям
 * USER/ADMIN.</p>
 */
@SpringBootApplication
@EnableAsync
public class ProjecthubApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProjecthubApplication.class, args);
    }
}
