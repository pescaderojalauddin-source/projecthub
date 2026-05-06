package com.example.projecthub.api;

import com.example.projecthub.api.dto.PageDto;
import com.example.projecthub.api.dto.ProjectDto;
import com.example.projecthub.api.dto.TaskDto;
import com.example.projecthub.dto.ProjectForm;
import com.example.projecthub.entity.Project;
import com.example.projecthub.entity.User;
import com.example.projecthub.service.CurrentUserService;
import com.example.projecthub.service.ProjectService;
import com.example.projecthub.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * RESTful API для проектов. Возвращает JSON, аутентификация — basic auth + сессия из браузера.
 *
 * <p>Все эндпоинты соблюдают RBAC: USER видит/правит только свои проекты, ADMIN — все.</p>
 */
@RestController
@RequestMapping("/api/v1/projects")
@PreAuthorize("isAuthenticated()")
@Tag(name = "Projects", description = "REST API для проектов")
public class ProjectApiController {

    private static final int MAX_PAGE_SIZE = 100;

    private final ProjectService projectService;
    private final TaskService taskService;
    private final CurrentUserService currentUserService;

    public ProjectApiController(ProjectService projectService,
                                TaskService taskService,
                                CurrentUserService currentUserService) {
        this.projectService = projectService;
        this.taskService = taskService;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    @Operation(summary = "Список проектов",
            description = "USER видит свои, ADMIN — все. Поддерживает поиск по названию.")
    public PageDto<ProjectDto> list(@RequestParam(defaultValue = "0") int page,
                                    @RequestParam(defaultValue = "20") int size,
                                    @RequestParam(required = false) String search) {
        User current = currentUserService.getCurrent();
        int safeSize = Math.min(Math.max(1, size), MAX_PAGE_SIZE);
        Page<Project> projects = projectService.listForUser(current, search,
                PageRequest.of(Math.max(0, page), safeSize));
        return PageDto.of(projects.map(ProjectDto::from));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Получить проект по id")
    public ProjectDto get(@PathVariable Long id) {
        User current = currentUserService.getCurrent();
        return ProjectDto.from(projectService.getByIdForUser(id, current));
    }

    @PostMapping
    @Operation(summary = "Создать проект")
    public ResponseEntity<ProjectDto> create(@Valid @RequestBody ProjectForm form) {
        User current = currentUserService.getCurrent();
        Project saved = projectService.create(form, current);
        return ResponseEntity.status(HttpStatus.CREATED).body(ProjectDto.from(saved));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Обновить проект")
    public ProjectDto update(@PathVariable Long id, @Valid @RequestBody ProjectForm form) {
        User current = currentUserService.getCurrent();
        return ProjectDto.from(projectService.update(id, form, current));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Удалить проект")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        User current = currentUserService.getCurrent();
        projectService.delete(id, current);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/tasks")
    @Operation(summary = "Задачи проекта")
    public PageDto<TaskDto> tasks(@PathVariable Long id,
                                  @RequestParam(defaultValue = "0") int page,
                                  @RequestParam(defaultValue = "20") int size) {
        User current = currentUserService.getCurrent();
        Project project = projectService.getByIdForUser(id, current);
        int safeSize = Math.min(Math.max(1, size), MAX_PAGE_SIZE);
        return PageDto.of(taskService.listForProject(project, null,
                        PageRequest.of(Math.max(0, page), safeSize))
                .map(TaskDto::from));
    }
}
