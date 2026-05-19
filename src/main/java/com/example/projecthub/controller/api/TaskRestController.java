package com.example.projecthub.controller.api;

import com.example.projecthub.dto.TaskForm;
import com.example.projecthub.dto.api.ChangeStatusRequest;
import com.example.projecthub.dto.api.TaskDto;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

// rEST API для задач. Все эндпоинты требуют аутентификацию; RBAC — через сервисный слой
@RestController
@RequestMapping("/api/v1")
@Tag(name = "Tasks", description = "CRUD задач, смена статуса")
public class TaskRestController {

    private final TaskService taskService;
    private final ProjectService projectService;
    private final CurrentUserService currentUserService;

    public TaskRestController(TaskService taskService,
                              ProjectService projectService,
                              CurrentUserService currentUserService) {
        this.taskService = taskService;
        this.projectService = projectService;
        this.currentUserService = currentUserService;
    }

    // список задач проекта с фильтром по статусу и пагинацией
    @GetMapping("/projects/{projectId}/tasks")
    @Operation(summary = "Список задач проекта")
    public Page<TaskDto> listByProject(@PathVariable Long projectId,
                                       @RequestParam(value = "status", required = false) TaskStatus status,
                                       @RequestParam(value = "page", defaultValue = "0") int page,
                                       @RequestParam(value = "size", defaultValue = "20") int size) {
        User current = currentUserService.getCurrent();
        Project project = projectService.getByIdForUser(projectId, current);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "deadline"));
        return taskService.listForProject(project, status, pageable).map(TaskDto::of);
    }

    // создание задачи в проекте
    @PostMapping("/projects/{projectId}/tasks")
    @Operation(summary = "Создать задачу")
    public ResponseEntity<TaskDto> create(@PathVariable Long projectId,
                                          @Valid @RequestBody TaskForm form) {
        User current = currentUserService.getCurrent();
        Project project = projectService.getByIdForUser(projectId, current);
        Task task = taskService.create(project, form, current);
        return ResponseEntity
                .created(java.net.URI.create("/api/v1/tasks/" + task.getId()))
                .body(TaskDto.of(task));
    }

    // получить задачу по ID
    @GetMapping("/tasks/{id}")
    @Operation(summary = "Получить задачу")
    public TaskDto get(@PathVariable Long id) {
        User current = currentUserService.getCurrent();
        return TaskDto.of(taskService.getByIdForUser(id, current));
    }

    // обновить задачу
    @PutMapping("/tasks/{id}")
    @Operation(summary = "Обновить задачу")
    public TaskDto update(@PathVariable Long id, @Valid @RequestBody TaskForm form) {
        User current = currentUserService.getCurrent();
        return TaskDto.of(taskService.update(id, form, current));
    }

    // сменить статус задачи
    @PatchMapping("/tasks/{id}/status")
    @Operation(summary = "Сменить статус задачи")
    public TaskDto changeStatus(@PathVariable Long id, @Valid @RequestBody ChangeStatusRequest request) {
        User current = currentUserService.getCurrent();
        return TaskDto.of(taskService.changeStatus(id, request.status(), current));
    }

    // удалить задачу
    @DeleteMapping("/tasks/{id}")
    @Operation(summary = "Удалить задачу")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        User current = currentUserService.getCurrent();
        taskService.delete(id, current);
    }
}
