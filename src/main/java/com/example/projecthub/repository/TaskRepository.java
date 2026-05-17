package com.example.projecthub.repository;

import com.example.projecthub.entity.Project;
import com.example.projecthub.entity.Task;
import com.example.projecthub.entity.TaskStatus;
import com.example.projecthub.entity.User;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.history.RevisionRepository;

/**
 * Репозиторий задач. Поддерживает выборку по проекту, фильтрацию по статусу и
 * счётчики для сводной статистики.
 *
 * <p>Списочные методы предзагружают {@code assignee} через {@link EntityGraph} —
 * исключает N+1 при рендеринге задач (имя исполнителя на странице проекта/REST DTO).
 *
 * <p>Расширен {@link RevisionRepository} — это даёт доступ к Hibernate Envers ревизиям:
 * {@code findRevisions(id)}, {@code findLastChangeRevision(id)} и др. Используется
 * на странице истории задачи.
 */
public interface TaskRepository extends JpaRepository<Task, Long>, RevisionRepository<Task, Long, Integer> {

    /** Постраничный список всех задач проекта. */
    @EntityGraph(attributePaths = {"assignee", "tags"})
    Page<Task> findAllByProject(Project project, Pageable pageable);

    /** Постраничный список задач проекта с фильтром по статусу. */
    @EntityGraph(attributePaths = {"assignee", "tags"})
    Page<Task> findAllByProjectAndStatus(Project project, TaskStatus status, Pageable pageable);

    /** Полный список задач проекта (используется в REST-ответах и канбане). */
    @EntityGraph(attributePaths = {"assignee", "tags"})
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
    @EntityGraph(attributePaths = {"assignee", "project", "project.owner", "tags"})
    Optional<Task> findById(Long id);

    /**
     * Для прогресс-бара: количество задач по статусам в каждом из проектов.
     * Возвращает строки {@code [projectId, status, count]} одним SQL-запросом — без N+1.
     */
    @Query("""
            SELECT t.project.id, t.status, COUNT(t)
            FROM Task t
            WHERE t.project.id IN :projectIds
            GROUP BY t.project.id, t.status
            """)
    List<Object[]> countByProjectIdGroupByStatus(Collection<Long> projectIds);

    /** Задачи, назначенные на пользователя, с фильтром по статусу (для дашборда). */
    @EntityGraph(attributePaths = {"project", "assignee"})
    List<Task> findTop10ByAssigneeAndStatusOrderByDeadlineAsc(User assignee, TaskStatus status);

    /** Задачи на пользователе с дедлайном «сегодня» и незавершённые. */
    @EntityGraph(attributePaths = {"project", "assignee"})
    List<Task> findTop10ByAssigneeAndDeadlineAndStatusNotInOrderByDeadlineAsc(
            User assignee, LocalDate deadline, Collection<TaskStatus> excluded);

    /** Просроченные задачи на пользователе (дедлайн в прошлом, не DONE). */
    @EntityGraph(attributePaths = {"project", "assignee"})
    List<Task> findTop10ByAssigneeAndDeadlineBeforeAndStatusNotInOrderByDeadlineAsc(
            User assignee, LocalDate deadline, Collection<TaskStatus> excluded);

    /** Счётчик задач на пользователя по статусу. */
    long countByAssigneeAndStatus(User assignee, TaskStatus status);

    /** Счётчик задач на пользователя с дедлайном «сегодня», не DONE. */
    long countByAssigneeAndDeadlineAndStatusNotIn(
            User assignee, LocalDate deadline, Collection<TaskStatus> excluded);

    /** Счётчик просроченных задач на пользователе. */
    long countByAssigneeAndDeadlineBeforeAndStatusNotIn(
            User assignee, LocalDate deadline, Collection<TaskStatus> excluded);

    /** Глобальный поиск по подстроке названия/описания задач. */
    @EntityGraph(attributePaths = {"project", "assignee"})
    @Query("""
            SELECT t FROM Task t
            WHERE LOWER(t.title) LIKE LOWER(CONCAT('%', :q, '%'))
               OR LOWER(t.description) LIKE LOWER(CONCAT('%', :q, '%'))
            ORDER BY t.id DESC
            """)
    List<Task> searchByText(String q, Pageable pageable);

    /** Глобальный поиск, ограниченный задачами в проектах указанного владельца (для USER). */
    @EntityGraph(attributePaths = {"project", "assignee"})
    @Query("""
            SELECT t FROM Task t
            WHERE t.project.owner = :owner
              AND (LOWER(t.title) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(t.description) LIKE LOWER(CONCAT('%', :q, '%')))
            ORDER BY t.id DESC
            """)
    List<Task> searchByTextForOwner(String q, User owner, Pageable pageable);

    /** Для дашбордного донат-чарта: распределение задач на пользователе по статусам одним SQL. */
    @Query("""
            SELECT t.status, COUNT(t) FROM Task t
            WHERE t.assignee = :assignee
            GROUP BY t.status
            """)
    List<Object[]> countByAssigneeGroupByStatus(User assignee);

    /** Для бар-чарта «Готово за период»: ежедневные количества DONE-задач пользователя по updated_at. */
    @Query("""
            SELECT CAST(t.updatedAt AS date) AS day, COUNT(t)
            FROM Task t
            WHERE t.assignee = :assignee
              AND t.status = com.example.projecthub.entity.TaskStatus.DONE
              AND t.updatedAt >= :from
            GROUP BY CAST(t.updatedAt AS date)
            ORDER BY day ASC
            """)
    List<Object[]> countDoneByAssigneeSince(User assignee, LocalDateTime from);

    /** Задачи проекта с заданным диапазоном дедлайнов (для календаря). */
    @Query("""
            SELECT t FROM Task t
            WHERE t.project = :project
              AND t.deadline BETWEEN :from AND :to
            ORDER BY t.deadline ASC
            """)
    List<Task> findByProjectAndDeadlineBetween(Project project, LocalDate from, LocalDate to);
}
