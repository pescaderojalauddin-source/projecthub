package com.example.projecthub.dto.api;

import com.example.projecthub.entity.Project;
import com.example.projecthub.entity.ProjectStatus;
import java.time.LocalDateTime;

/**
 * REST-представление проекта. Безопасно сериализуется (нет ссылок на ленивые ассоциации).
 */
public record ProjectDto(
        Long id,
        String title,
        String description,
        ProjectStatus status,
        Long ownerId,
        String ownerLogin,
        LocalDateTime createdAt
) {

    /** Конвертирует JPA-сущность в DTO. */
    public static ProjectDto of(Project project) {
        return new ProjectDto(
                project.getId(),
                project.getTitle(),
                project.getDescription(),
                project.getStatus(),
                project.getOwner() != null ? project.getOwner().getId() : null,
                project.getOwner() != null ? project.getOwner().getLogin() : null,
                project.getCreatedAt()
        );
    }
}
