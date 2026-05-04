package com.example.projecthub.api;

import com.example.projecthub.api.dto.TaskDto;
import com.example.projecthub.dto.TaskForm;
import com.example.projecthub.entity.Project;
import com.example.projecthub.entity.Task;
import com.example.projecthub.entity.TaskStatus;
import com.example.projecthub.entity.User;
import com.example.projecthub.service.CurrentUserService;
import com.example.projecthub.service.ProjectService;
import com.example.projecthub.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * RESTful API для задач: создание/чтение/обновление/удаление и смена статуса.
 *
 * <p>Создание идёт в контексте проекта: {@code POST /api/v1/projects/{id}/tasks} — но удобнее
 * иметь и top-level {@code POST /api/v1/tasks} для clients, передающих projectId в теле.</p>
 */
@RestController
@RequestMapping("/api/v1/tasks")
@PreAuthorize("isAuthenticated()")
@Tag(name = "Tasks", description = "REST API для задач")
public class TaskApiController {

    private final TaskService taskService;
    private final ProjectService projectService;
    private final CurrentUserService currentUserService;

    public TaskApiController(TaskService taskService,
                             ProjectService projectService,
                             CurrentUserService currentUserService) {
        this.taskService = taskService;
        this.projectService = projectService;
        this.currentUserService = currentUserService;
    }

    @GetMapping("/{id}")
    @Operation(summary = "Получить задачу по id")
    public TaskDto get(@PathVariable Long id) {
        User current = currentUserService.getCurrent();
        return TaskDto.from(taskService.getByIdForUser(id, current));
    }

    @PostMapping
    @Operation(summary = "Создать задачу в проекте",
            description = "Тело запроса должно содержать projectId; иначе вернётся 400.")
    public ResponseEntity<TaskDto> create(@Valid @RequestBody TaskCreateRequest request) {
        if (request.projectId() == null) {
            return ResponseEntity.badRequest().build();
        }
        User current = currentUserService.getCurrent();
        Project project = projectService.getByIdForUser(request.projectId(), current);
        TaskForm form = new TaskForm();
        form.setTitle(request.title());
        form.setDescription(request.description());
        form.setStatus(request.status() != null ? request.status() : TaskStatus.TODO);
        form.setDeadline(request.deadline());
        form.setAssigneeId(request.assigneeId());
        Task saved = taskService.create(project, form, current);
        return ResponseEntity.status(HttpStatus.CREATED).body(TaskDto.from(saved));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Обновить задачу")
    public TaskDto update(@PathVariable Long id, @Valid @RequestBody TaskUpdateRequest request) {
        User current = currentUserService.getCurrent();
        TaskForm form = new TaskForm();
        form.setTitle(request.title());
        form.setDescription(request.description());
        form.setStatus(request.status());
        form.setDeadline(request.deadline());
        form.setAssigneeId(request.assigneeId());
        return TaskDto.from(taskService.update(id, form, current));
    }

    @PostMapping("/{id}/status")
    @Operation(summary = "Сменить статус задачи")
    public TaskDto changeStatus(@PathVariable Long id,
                                @RequestBody TaskStatusRequest request) {
        User current = currentUserService.getCurrent();
        return TaskDto.from(taskService.changeStatus(id, request.status(), current));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Удалить задачу")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        User current = currentUserService.getCurrent();
        taskService.delete(id, current);
        return ResponseEntity.noContent().build();
    }

    /** Тело запроса на создание задачи. */
    public record TaskCreateRequest(
            Long projectId,
            @jakarta.validation.constraints.NotBlank String title,
            String description,
            TaskStatus status,
            java.time.LocalDate deadline,
            Long assigneeId) { }

    /** Тело запроса на обновление задачи. */
    public record TaskUpdateRequest(
            @jakarta.validation.constraints.NotBlank String title,
            String description,
            @jakarta.validation.constraints.NotNull TaskStatus status,
            java.time.LocalDate deadline,
            Long assigneeId) { }

    /** Тело запроса на смену статуса. */
    public record TaskStatusRequest(@jakarta.validation.constraints.NotNull TaskStatus status) { }
}
