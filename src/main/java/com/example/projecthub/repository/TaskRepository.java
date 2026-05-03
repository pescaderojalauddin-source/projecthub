package com.example.projecthub.repository;

import com.example.projecthub.entity.Project;
import com.example.projecthub.entity.Task;
import com.example.projecthub.entity.TaskStatus;
import com.example.projecthub.entity.User;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TaskRepository extends JpaRepository<Task, Long> {

    Page<Task> findAllByProject(Project project, Pageable pageable);

    Page<Task> findAllByProjectAndStatus(Project project, TaskStatus status, Pageable pageable);

    List<Task> findAllByProject(Project project);

    long countByProject(Project project);

    long countByStatus(TaskStatus status);

    /** Открытые задачи пользователя: где он исполнитель ИЛИ владелец проекта. */
    @Query("select t from Task t where t.status <> com.example.projecthub.entity.TaskStatus.DONE "
            + "and (t.assignee = :user or t.project.owner = :user) "
            + "order by t.deadline asc nulls last, t.id asc")
    List<Task> findOpenForUser(@Param("user") User user);

    /** Просроченные открытые задачи пользователя (исполнитель или владелец проекта). */
    @Query("select t from Task t where t.status <> com.example.projecthub.entity.TaskStatus.DONE "
            + "and t.deadline is not null and t.deadline < :today "
            + "and (t.assignee = :user or t.project.owner = :user) "
            + "order by t.deadline asc")
    List<Task> findOverdueForUser(@Param("user") User user, @Param("today") LocalDate today);

    /** Задачи пользователя с дедлайном до указанной даты включительно (для виджета «на этой неделе»). */
    @Query("select t from Task t where t.status <> com.example.projecthub.entity.TaskStatus.DONE "
            + "and t.deadline is not null and t.deadline >= :from and t.deadline <= :to "
            + "and (t.assignee = :user or t.project.owner = :user) "
            + "order by t.deadline asc")
    List<Task> findUpcomingForUser(@Param("user") User user,
                                   @Param("from") LocalDate from,
                                   @Param("to") LocalDate to);
}
