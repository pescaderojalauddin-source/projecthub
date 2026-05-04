package com.example.projecthub.api.dto;

import com.example.projecthub.entity.Task;
import com.example.projecthub.entity.TaskStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO задачи для REST API.
 */
@Schema(description = "Задача")
public record TaskDto(
        @Schema(example = "101") Long id,
        @Schema(example = "Написать SQL-скрипты миграции") String title,
        @Schema(example = "Алёша должен подготовить...") String description,
        @Schema(example = "IN_PROGRESS") TaskStatus status,
        @Schema(example = "2026-05-01") LocalDate deadline,
        @Schema(example = "10") Long projectId,
        @Schema(example = "Миграция БД") String projectTitle,
        @Schema(example = "2") Long assigneeId,
        @Schema(example = "maria") String assigneeLogin,
        LocalDateTime createdAt
) {
    public static TaskDto from(Task t) {
        return new TaskDto(
                t.getId(),
                t.getTitle(),
                t.getDescription(),
                t.getStatus(),
                t.getDeadline(),
                t.getProject() != null ? t.getProject().getId() : null,
                t.getProject() != null ? t.getProject().getTitle() : null,
                t.getAssignee() != null ? t.getAssignee().getId() : null,
                t.getAssignee() != null ? t.getAssignee().getLogin() : null,
                t.getCreatedAt()
        );
    }
}
