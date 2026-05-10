package com.example.projecthub.dto;

import com.example.projecthub.entity.ProjectStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Форма создания/редактирования проекта. */
public class ProjectForm {

    private Long id;

    @NotBlank(message = "Название обязательно")
    @Size(min = 1, max = 200, message = "Название: до 200 символов")
    private String title;

    @Size(max = 2000, message = "Описание: до 2000 символов")
    private String description;

    @NotNull(message = "Статус обязателен")
    private ProjectStatus status = ProjectStatus.ACTIVE;

    @Size(max = 8, message = "Эмодзи слишком длинное")
    private String emoji;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public ProjectStatus getStatus() {
        return status;
    }

    public void setStatus(ProjectStatus status) {
        this.status = status;
    }

    public String getEmoji() {
        return emoji;
    }

    public void setEmoji(String emoji) {
        this.emoji = emoji;
    }
}
