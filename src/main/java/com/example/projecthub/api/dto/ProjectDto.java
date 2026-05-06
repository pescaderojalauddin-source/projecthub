package com.example.projecthub.api.dto;

import com.example.projecthub.entity.Project;
import com.example.projecthub.entity.ProjectStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/**
 * DTO проекта для REST API.
 */
@Schema(description = "Проект")
public record ProjectDto(
        @Schema(example = "10") Long id,
        @Schema(example = "Миграция БД") String title,
        @Schema(example = "Перевод схемы на Postgres 15") String description,
        @Schema(example = "ACTIVE") ProjectStatus status,
        @Schema(example = "ivan") String ownerLogin,
        @Schema(example = "1") Long ownerId,
        LocalDateTime createdAt
) {
    public static ProjectDto from(Project p) {
        return new ProjectDto(
                p.getId(),
                p.getTitle(),
                p.getDescription(),
                p.getStatus(),
                p.getOwner() != null ? p.getOwner().getLogin() : null,
                p.getOwner() != null ? p.getOwner().getId() : null,
                p.getCreatedAt()
        );
    }
}
