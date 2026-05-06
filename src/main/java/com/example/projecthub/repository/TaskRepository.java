package com.example.projecthub.repository;

import com.example.projecthub.entity.Project;
import com.example.projecthub.entity.Task;
import com.example.projecthub.entity.TaskStatus;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Репозиторий задач. Поддерживает выборку по проекту, фильтрацию по статусу и
 * счётчики для сводной статистики.
 */
public interface TaskRepository extends JpaRepository<Task, Long> {

    /** Постраничный список всех задач проекта. */
    Page<Task> findAllByProject(Project project, Pageable pageable);

    /** Постраничный список задач проекта с фильтром по статусу. */
    Page<Task> findAllByProjectAndStatus(Project project, TaskStatus status, Pageable pageable);

    /** Полный список задач проекта (используется в REST-ответах). */
    List<Task> findAllByProject(Project project);

    /** Количество задач в проекте. */
    long countByProject(Project project);

    /** Количество задач в указанном статусе (для сводной статистики). */
    long countByStatus(TaskStatus status);
}
