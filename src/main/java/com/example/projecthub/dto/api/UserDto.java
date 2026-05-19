package com.example.projecthub.dto.api;

import com.example.projecthub.entity.Role;
import com.example.projecthub.entity.User;
import java.time.LocalDateTime;

// rEST-представление юзера (без пароля)
public record UserDto(
        Long id,
        String login,
        Role role,
        LocalDateTime createdAt
) {

    // конвертирует JPA-сущность в DTO
    public static UserDto of(User user) {
        return new UserDto(user.getId(), user.getLogin(), user.getRole(), user.getCreatedAt());
    }
}
