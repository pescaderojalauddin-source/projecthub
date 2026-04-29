package com.example.projecthub.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Базовое описание API для Swagger UI (springdoc). */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI projectHubOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("ProjectHub API")
                        .description("REST/HTML интерфейс системы управления проектами и задачами.")
                        .version("v1")
                        .contact(new Contact().name("ProjectHub"))
                        .license(new License().name("MIT")));
    }
}
