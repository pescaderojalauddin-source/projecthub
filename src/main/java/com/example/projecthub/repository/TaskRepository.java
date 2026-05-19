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

// репозиторий задач
public interface TaskRepository extends JpaRepository<Task, Long>, RevisionRepository<Task, Long, Integer> {

    // постраничный список всех задач проекта
    @EntityGraph(attributePaths = {"assignee", "tags"})
    Page<Task> findAllByProject(Project project, Pageable pageable);

    // постраничный список задач проекта с фильтром по статусу
    @EntityGraph(attributePaths = {"assignee", "tags"})
    Page<Task> findAllByProjectAndStatus(Project project, TaskStatus status, Pageable pageable);

    // полный список задач проекта (используется в REST-ответах и канбане)
    @EntityGraph(attributePaths = {"assignee", "tags"})
    List<Task> findAllByProject(Project project);

    // количество задач в проекте
    long countByProject(Project project);

    // количество задач в указанном статусе (для сводной статистики)
    long countByStatus(TaskStatus status);

    // сингл-выборка с жадной подгрузкой исполнителя, проекта и владельца проекта шаблон страницы задачи
    @Override
    @EntityGraph(attributePaths = {"assignee", "project", "project.owner", "tags"})
    Optional<Task> findById(Long id);

    // для прогресс-бара: количество задач по статусам в каждом из проектов возвращает строки
    @Query("""
            SELECT t.project.id, t.status, COUNT(t)
            FROM Task t
            WHERE t.project.id IN :projectIds
            GROUP BY t.project.id, t.status
            """)
    List<Object[]> countByProjectIdGroupByStatus(Collection<Long> projectIds);

    // задачи, назначенные на юзера, с фильтром по статусу (для дашборда)
    @EntityGraph(attributePaths = {"project", "assignee"})
    List<Task> findTop10ByAssigneeAndStatusOrderByDeadlineAsc(User assignee, TaskStatus status);

    // задачи на пользователе с дедлайном «сегодня» и незавершённые
    @EntityGraph(attributePaths = {"project", "assignee"})
    List<Task> findTop10ByAssigneeAndDeadlineAndStatusNotInOrderByDeadlineAsc(
            User assignee, LocalDate deadline, Collection<TaskStatus> excluded);

    // просроченные задачи на пользователе (дедлайн в прошлом, не DONE)
    @EntityGraph(attributePaths = {"project", "assignee"})
    List<Task> findTop10ByAssigneeAndDeadlineBeforeAndStatusNotInOrderByDeadlineAsc(
            User assignee, LocalDate deadline, Collection<TaskStatus> excluded);

    // счётчик задач на юзера по статусу
    long countByAssigneeAndStatus(User assignee, TaskStatus status);

    // счётчик задач на юзера с дедлайном «сегодня», не DONE
    long countByAssigneeAndDeadlineAndStatusNotIn(
            User assignee, LocalDate deadline, Collection<TaskStatus> excluded);

    // счётчик просроченных задач на пользователе
    long countByAssigneeAndDeadlineBeforeAndStatusNotIn(
            User assignee, LocalDate deadline, Collection<TaskStatus> excluded);

    // глобальный поиск по подстроке названия/описания задач
    @EntityGraph(attributePaths = {"project", "assignee"})
    @Query("""
            SELECT t FROM Task t
            WHERE LOWER(t.title) LIKE LOWER(CONCAT('%', :q, '%'))
               OR LOWER(t.description) LIKE LOWER(CONCAT('%', :q, '%'))
            ORDER BY t.id DESC
            """)
    List<Task> searchByText(String q, Pageable pageable);

    // глобальный поиск, ограниченный задачами в проектах указанного владельца (для USER)
    @EntityGraph(attributePaths = {"project", "assignee"})
    @Query("""
            SELECT t FROM Task t
            WHERE t.project.owner = :owner
              AND (LOWER(t.title) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(t.description) LIKE LOWER(CONCAT('%', :q, '%')))
            ORDER BY t.id DESC
            """)
    List<Task> searchByTextForOwner(String q, User owner, Pageable pageable);

    // для дашбордного донат-чарта: распределение задач на пользователе по статусам одним SQL
    @Query("""
            SELECT t.status, COUNT(t) FROM Task t
            WHERE t.assignee = :assignee
            GROUP BY t.status
            """)
    List<Object[]> countByAssigneeGroupByStatus(User assignee);

    // для бар-чарта «Готово за период»: ежедневные количества DONE-задач юзера по updated_at
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

    // задачи проекта с заданным диапазоном дедлайнов (для календаря)
    @Query("""
            SELECT t FROM Task t
            WHERE t.project = :project
              AND t.deadline BETWEEN :from AND :to
            ORDER BY t.deadline ASC
            """)
    List<Task> findByProjectAndDeadlineBetween(Project project, LocalDate from, LocalDate to);
}
