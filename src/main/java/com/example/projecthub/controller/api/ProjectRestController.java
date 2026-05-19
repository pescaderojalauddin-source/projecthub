package com.example.projecthub.controller.api;

import com.example.projecthub.dto.ProjectForm;
import com.example.projecthub.dto.api.ProjectDto;
import com.example.projecthub.entity.Project;
import com.example.projecthub.entity.User;
import com.example.projecthub.service.CurrentUserService;
import com.example.projecthub.service.ProjectService;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

// rEST API для проектов
@RestController
@RequestMapping("/api/v1/projects")
@Tag(name = "Projects", description = "CRUD операций над проектами")
public class ProjectRestController {

    private final ProjectService projectService;
    private final CurrentUserService currentUserService;

    public ProjectRestController(ProjectService projectService, CurrentUserService currentUserService) {
        this.projectService = projectService;
        this.currentUserService = currentUserService;
    }

    // постраничный список проектов
    @GetMapping
    @Operation(summary = "Список проектов")
    public Page<ProjectDto> list(@RequestParam(value = "search", required = false) String search,
                                 @RequestParam(value = "page", defaultValue = "0") int page,
                                 @RequestParam(value = "size", defaultValue = "20") int size) {
        User current = currentUserService.getCurrent();
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return projectService.listForUser(current, search, pageable).map(ProjectDto::of);
    }

    // детали проекта по ID
    @GetMapping("/{id}")
    @Operation(summary = "Получить проект")
    public ProjectDto get(@PathVariable Long id) {
        User current = currentUserService.getCurrent();
        return ProjectDto.of(projectService.getByIdForUser(id, current));
    }

    // создание проекта. Возвращает 201 Created с заголовком Location
    @PostMapping
    @Operation(summary = "Создать проект")
    public ResponseEntity<ProjectDto> create(@Valid @RequestBody ProjectForm form) {
        User current = currentUserService.getCurrent();
        Project project = projectService.create(form, current);
        return ResponseEntity
                .created(java.net.URI.create("/api/v1/projects/" + project.getId()))
                .body(ProjectDto.of(project));
    }

    // обновление полей проекта
    @PutMapping("/{id}")
    @Operation(summary = "Обновить проект")
    public ProjectDto update(@PathVariable Long id, @Valid @RequestBody ProjectForm form) {
        User current = currentUserService.getCurrent();
        return ProjectDto.of(projectService.update(id, form, current));
    }

    // удаление проекта
    @DeleteMapping("/{id}")
    @Operation(summary = "Удалить проект")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        User current = currentUserService.getCurrent();
        projectService.delete(id, current);
    }
}
