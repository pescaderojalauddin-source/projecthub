package com.example.projecthub.dto.api;

import com.example.projecthub.entity.Role;
import jakarta.validation.constraints.NotNull;

/** Запрос на смену роли пользователя. */
public record ChangeRoleRequest(@NotNull Role role) {
}
