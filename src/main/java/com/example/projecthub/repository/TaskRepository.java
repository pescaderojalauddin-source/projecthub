package com.example.projecthub.repository;

import com.example.projecthub.entity.Project;
import com.example.projecthub.entity.Task;
import com.example.projecthub.entity.TaskStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Репозиторий задач. Поддерживает выборку по проекту, фильтрацию по статусу и
 * счётчики для сводной статистики.
 *
 * <p>Списочные методы предзагружают {@code assignee} через {@link EntityGraph} —
 * исключает N+1 при рендеринге задач (имя исполнителя на странице проекта/REST DTO).
 */
public interface TaskRepository extends JpaRepository<Task, Long> {

    /** Постраничный список всех задач проекта. */
    @EntityGraph(attributePaths = "assignee")
    Page<Task> findAllByProject(Project project, Pageable pageable);

    /** Постраничный список задач проекта с фильтром по статусу. */
    @EntityGraph(attributePaths = "assignee")
    Page<Task> findAllByProjectAndStatus(Project project, TaskStatus status, Pageable pageable);

    /** Полный список задач проекта (используется в REST-ответах). */
    @EntityGraph(attributePaths = "assignee")
    List<Task> findAllByProject(Project project);

    /** Количество задач в проекте. */
    long countByProject(Project project);

    /** Количество задач в указанном статусе (для сводной статистики). */
    long countByStatus(TaskStatus status);

    /**
     * Сингл-выборка с жадной подгрузкой исполнителя, проекта и владельца проекта.
     * Шаблон страницы задачи обращается к {@code task.project.title} и {@code task.assignee.login}
     * уже после закрытия транзакции (open-in-view=false). А {@code project.owner}
     * нужен в {@code TaskService.ensureAccessible()}.
     */
    @Override
    @EntityGraph(attributePaths = {"assignee", "project", "project.owner"})
    Optional<Task> findById(Long id);
}
