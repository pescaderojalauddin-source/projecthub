package com.example.projecthub.dto.api;

import com.example.projecthub.entity.Role;
import jakarta.validation.constraints.NotNull;

// запрос на смену роли юзера
public record ChangeRoleRequest(@NotNull Role role) {
}
