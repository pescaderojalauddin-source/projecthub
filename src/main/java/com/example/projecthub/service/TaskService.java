package com.example.projecthub.service;

import com.example.projecthub.dto.TaskForm;
import com.example.projecthub.entity.Project;
import com.example.projecthub.entity.Role;
import com.example.projecthub.entity.Task;
import com.example.projecthub.entity.TaskStatus;
import com.example.projecthub.entity.User;
import com.example.projecthub.exception.AccessDeniedAppException;
import com.example.projecthub.exception.ResourceNotFoundException;
import com.example.projecthub.repository.TaskRepository;
import com.example.projecthub.repository.UserRepository;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.history.Revisions;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Сервис задач: CRUD, фильтрация по статусу, RBAC. */
@Service
@Transactional
public class TaskService {

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final ProjectService projectService;

    public TaskService(TaskRepository taskRepository,
                       UserRepository userRepository,
                       ProjectService projectService) {
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
        this.projectService = projectService;
    }

    /** Постраничный список задач в проекте с опциональным фильтром по статусу. */
    @Transactional(readOnly = true)
    public Page<Task> listForProject(Project project, TaskStatus statusFilter, Pageable pageable) {
        if (statusFilter != null) {
            return taskRepository.findAllByProjectAndStatus(project, statusFilter, pageable);
        }
        return taskRepository.findAllByProject(project, pageable);
    }

    /** Полный список задач проекта без пагинации (для канбан-доски). */
    @Transactional(readOnly = true)
    public List<Task> findAllForProject(Project project) {
        return taskRepository.findAllByProject(project);
    }

    /**
     * Возвращает все ревизии задачи (Hibernate Envers). Используется на странице
     * истории изменений {@code GET /tasks/{id}/history}.
     *
     * <p>Доступ к истории контролируется так же, как доступ к самой задаче — поэтому
     * сначала вызываем {@link #getByIdForUser(Long, User)}.
     */
    @Transactional(readOnly = true)
    public Revisions<Integer, Task> findRevisionsForUser(Long id, User actor) {
        getByIdForUser(id, actor); // RBAC
        return taskRepository.findRevisions(id);
    }

    /** Возвращает задачу с проверкой прав доступа. */
    @Transactional(readOnly = true)
    public Task getByIdForUser(Long id, User user) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Задача не найдена: id=" + id));
        ensureAccessible(task, user);
        return task;
    }

    /** Создание задачи в рамках проекта. */
    public Task create(Project project, TaskForm form, User actor) {
        projectService.ensureAccessible(project, actor);
        User assignee = resolveAssignee(form.getAssigneeId());
        Task task = new Task(form.getTitle(), form.getDescription(), form.getStatus(),
                form.getDeadline(), project, assignee);
        task.setPriority(form.getPriority());
        task.setTags(parseTags(form.getTagsCsv()));
        return taskRepository.save(task);
    }

    /** Обновление полей задачи. */
    public Task update(Long id, TaskForm form, User actor) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Задача не найдена: id=" + id));
        ensureAccessible(task, actor);
        task.setTitle(form.getTitle());
        task.setDescription(form.getDescription());
        task.setStatus(form.getStatus());
        task.setDeadline(form.getDeadline());
        task.setAssignee(resolveAssignee(form.getAssigneeId()));
        task.setPriority(form.getPriority());
        task.getTags().clear();
        task.getTags().addAll(parseTags(form.getTagsCsv()));
        return taskRepository.save(task);
    }

    /**
     * Разбивает строку тегов (через запятую/пробел) в упорядоченный Set.
     * Дубли и пустие строки игнорируются. Каждый тег обрезается до 40 символов.
     */
    public static java.util.LinkedHashSet<String> parseTags(String csv) {
        java.util.LinkedHashSet<String> result = new java.util.LinkedHashSet<>();
        if (csv == null || csv.isBlank()) {
            return result;
        }
        for (String raw : csv.split("[,\\s]+")) {
            String t = raw.trim();
            if (t.isEmpty()) continue;
            if (t.startsWith("#")) t = t.substring(1);
            if (t.length() > 40) t = t.substring(0, 40);
            if (!t.isBlank()) result.add(t.toLowerCase());
        }
        return result;
    }

    /** Смена статуса задачи. */
    public Task changeStatus(Long id, TaskStatus newStatus, User actor) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Задача не найдена: id=" + id));
        ensureAccessible(task, actor);
        task.setStatus(newStatus);
        return taskRepository.save(task);
    }

    /** Удаление задачи (владельцем проекта, исполнителем или ADMIN). */
    public void delete(Long id, User actor) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Задача не найдена: id=" + id));
        ensureAccessible(task, actor);
        taskRepository.delete(task);
    }

    /** Доступ к задаче: владелец проекта, исполнитель или админ. */
    public void ensureAccessible(Task task, User user) {
        if (user.getRole() == Role.ADMIN) {
            return;
        }
        Long ownerId = task.getProject().getOwner().getId();
        if (ownerId.equals(user.getId())) {
            return;
        }
        if (task.getAssignee() != null && user.getId().equals(task.getAssignee().getId())) {
            return;
        }
        throw new AccessDeniedAppException("Нет доступа к задаче: id=" + task.getId());
    }

    /** Разрешает ID исполнителя в сущность {@link User}; null — если исполнитель не назначен. */
    private User resolveAssignee(Long assigneeId) {
        if (assigneeId == null) {
            return null;
        }
        return userRepository.findById(assigneeId)
                .orElseThrow(() -> new ResourceNotFoundException("Исполнитель не найден: id=" + assigneeId));
    }

    /** Общее число задач (для сводной статистики). */
    @Transactional(readOnly = true)
    public long count() {
        return taskRepository.count();
    }

    /** Количество задач в указанном статусе (для сводной статистики). */
    @Transactional(readOnly = true)
    public long countByStatus(TaskStatus status) {
        return taskRepository.countByStatus(status);
    }
}
