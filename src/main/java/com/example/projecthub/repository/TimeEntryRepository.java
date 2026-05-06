package com.example.projecthub.repository;

import com.example.projecthub.entity.Task;
import com.example.projecthub.entity.TimeEntry;
import com.example.projecthub.entity.User;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TimeEntryRepository extends JpaRepository<TimeEntry, Long> {

    Optional<TimeEntry> findFirstByUserAndEndAtIsNull(User user);

    List<TimeEntry> findAllByTaskOrderByStartAtDesc(Task task);

    Page<TimeEntry> findAllByUserOrderByStartAtDesc(User user, Pageable pageable);

    List<TimeEntry> findAllByUserAndStartAtBetweenOrderByStartAtDesc(
            User user, LocalDateTime from, LocalDateTime to);

    @Query("select coalesce(sum(t.durationSeconds), 0) from TimeEntry t "
            + "where t.task = :task and t.endAt is not null")
    long sumDurationSecondsByTask(@Param("task") Task task);

    @Query("select coalesce(sum(t.durationSeconds), 0) from TimeEntry t "
            + "where t.user = :user and t.endAt is not null "
            + "and t.startAt >= :from and t.startAt < :to")
    long sumDurationSecondsByUserBetween(@Param("user") User user,
                                         @Param("from") LocalDateTime from,
                                         @Param("to") LocalDateTime to);

    @Query("select t.task.id as taskId, coalesce(sum(t.durationSeconds), 0) as total "
            + "from TimeEntry t where t.task.project.id = :projectId and t.endAt is not null "
            + "group by t.task.id")
    List<TaskTotal> sumDurationSecondsByProject(@Param("projectId") Long projectId);

    interface TaskTotal {
        Long getTaskId();
        Long getTotal();
    }
}
