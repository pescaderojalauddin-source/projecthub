package com.example.projecthub.dto.api;

import com.example.projecthub.entity.TaskStatus;
import jakarta.validation.constraints.NotNull;

/** Запрос на смену статуса задачи. */
public record ChangeStatusRequest(@NotNull TaskStatus status) {
}
