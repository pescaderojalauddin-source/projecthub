package com.example.projecthub.service;

import com.example.projecthub.dto.ProjectForm;
import com.example.projecthub.entity.Project;
import com.example.projecthub.entity.Role;
import com.example.projecthub.entity.User;
import com.example.projecthub.exception.AccessDeniedAppException;
import com.example.projecthub.exception.ResourceNotFoundException;
import com.example.projecthub.repository.ProjectRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Сервис проектов. Содержит RBAC-проверки: USER видит/редактирует только свои проекты,
 * ADMIN — все.
 */
@Service
@Transactional
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final AuditService auditService;

    public ProjectService(ProjectRepository projectRepository, AuditService auditService) {
        this.projectRepository = projectRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public Page<Project> listForUser(User user, String search, Pageable pageable) {
        boolean hasSearch = search != null && !search.isBlank();
        if (user.getRole() == Role.ADMIN) {
            return hasSearch
                    ? projectRepository.findAllByTitleContainingIgnoreCase(search.trim(), pageable)
                    : projectRepository.findAll(pageable);
        }
        return hasSearch
                ? projectRepository.findAllByOwnerAndTitleContainingIgnoreCase(user, search.trim(), pageable)
                : projectRepository.findAllByOwner(user, pageable);
    }

    @Transactional(readOnly = true)
    public Project getByIdForUser(Long id, User user) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Проект не найден: id=" + id));
        ensureAccessible(project, user);
        return project;
    }

    public Project create(ProjectForm form, User owner) {
        Project project = new Project(form.getTitle(), form.getDescription(), form.getStatus(), owner);
        Project saved = projectRepository.save(project);
        auditService.record("PROJECT_CREATED", "Project", saved.getId(),
                "title=" + saved.getTitle());
        return saved;
    }

    public Project update(Long id, ProjectForm form, User actor) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Проект не найден: id=" + id));
        ensureAccessible(project, actor);
        project.setTitle(form.getTitle());
        project.setDescription(form.getDescription());
        project.setStatus(form.getStatus());
        Project saved = projectRepository.save(project);
        auditService.record("PROJECT_UPDATED", "Project", saved.getId(),
                "status=" + saved.getStatus());
        return saved;
    }

    public void delete(Long id, User actor) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Проект не найден: id=" + id));
        ensureAccessible(project, actor);
        projectRepository.delete(project);
        auditService.record("PROJECT_DELETED", "Project", id, "title=" + project.getTitle());
    }

    /** Проверка прав на конкретный проект. */
    public void ensureAccessible(Project project, User user) {
        if (user.getRole() == Role.ADMIN) {
            return;
        }
        if (!project.getOwner().getId().equals(user.getId())) {
            throw new AccessDeniedAppException("Нет доступа к проекту: id=" + project.getId());
        }
    }

    @Transactional(readOnly = true)
    public long count() {
        return projectRepository.count();
    }
}
