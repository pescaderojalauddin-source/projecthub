package com.example.projecthub.dto;

import com.example.projecthub.entity.TaskPriority;
import com.example.projecthub.entity.TaskStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;

/** Форма создания/редактирования задачи. */
public class TaskForm {

    private Long id;

    @NotBlank(message = "Название обязательно")
    @Size(min = 1, max = 200, message = "Название: до 200 символов")
    private String title;

    @Size(max = 4000, message = "Описание: до 4000 символов")
    private String description;

    @NotNull(message = "Статус обязателен")
    private TaskStatus status = TaskStatus.TODO;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate deadline;

    private Long assigneeId;

    @NotNull(message = "Приоритет обязателен")
    private TaskPriority priority = TaskPriority.MEDIUM;

    /** Строка тегов через запятую или пробел. Разбивается в TaskController. */
    @Size(max = 500, message = "Теги: до 500 символов всего")
    private String tagsCsv = "";

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

    public TaskStatus getStatus() {
        return status;
    }

    public void setStatus(TaskStatus status) {
        this.status = status;
    }

    public LocalDate getDeadline() {
        return deadline;
    }

    public void setDeadline(LocalDate deadline) {
        this.deadline = deadline;
    }

    public Long getAssigneeId() {
        return assigneeId;
    }

    public void setAssigneeId(Long assigneeId) {
        this.assigneeId = assigneeId;
    }

    public TaskPriority getPriority() {
        return priority;
    }

    public void setPriority(TaskPriority priority) {
        this.priority = (priority != null) ? priority : TaskPriority.MEDIUM;
    }

    public String getTagsCsv() {
        return tagsCsv;
    }

    public void setTagsCsv(String tagsCsv) {
        this.tagsCsv = (tagsCsv != null) ? tagsCsv : "";
    }
}
