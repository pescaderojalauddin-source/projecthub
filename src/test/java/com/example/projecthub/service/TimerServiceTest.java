package com.example.projecthub.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.projecthub.entity.Project;
import com.example.projecthub.entity.ProjectStatus;
import com.example.projecthub.entity.Role;
import com.example.projecthub.entity.Task;
import com.example.projecthub.entity.TaskStatus;
import com.example.projecthub.entity.TimeEntry;
import com.example.projecthub.entity.User;
import com.example.projecthub.repository.TimeEntryRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TimerServiceTest {

    @Mock TimeEntryRepository repository;
    @Mock TaskService taskService;
    @Mock AuditService auditService;

    private TimerService timerService;
    private final Clock clock = Clock.fixed(Instant.parse("2026-04-29T12:00:00Z"), ZoneId.of("UTC"));

    private User user;
    private Project project;
    private Task taskA;
    private Task taskB;

    @BeforeEach
    void setUp() {
        timerService = new TimerService(repository, taskService, auditService, clock);
        user = new User("ivan", "h", Role.USER);
        user.setId(1L);
        project = new Project("P", null, ProjectStatus.ACTIVE, user);
        project.setId(10L);
        taskA = new Task("A", null, TaskStatus.TODO, null, project, user);
        taskA.setId(100L);
        taskB = new Task("B", null, TaskStatus.TODO, null, project, user);
        taskB.setId(101L);
    }

    @Test
    void startCreatesNewEntryWhenNoActive() {
        when(taskService.getByIdForUser(100L, user)).thenReturn(taskA);
        when(repository.findFirstByUserAndEndAtIsNull(user)).thenReturn(Optional.empty());
        when(repository.save(any(TimeEntry.class))).thenAnswer(inv -> inv.getArgument(0));

        TimeEntry started = timerService.start(100L, user);

        assertThat(started.getTask()).isSameAs(taskA);
        assertThat(started.getUser()).isSameAs(user);
        assertThat(started.isActive()).isTrue();
        verify(auditService).record(eq("TIMER_STARTED"), eq("Task"), eq(100L), anyString());
    }

    @Test
    void startReturnsExistingActiveForSameTask() {
        TimeEntry existing = new TimeEntry(taskA, user, timerService.now().minusMinutes(5));
        when(taskService.getByIdForUser(100L, user)).thenReturn(taskA);
        when(repository.findFirstByUserAndEndAtIsNull(user)).thenReturn(Optional.of(existing));

        TimeEntry result = timerService.start(100L, user);

        assertThat(result).isSameAs(existing);
        verify(repository, never()).save(any(TimeEntry.class));
        verify(auditService, never()).record(eq("TIMER_STARTED"), anyString(), any(), anyString());
    }

    @Test
    void startStopsPreviousActiveAndStartsNew() {
        TimeEntry existing = new TimeEntry(taskB, user, timerService.now().minusMinutes(10));
        when(taskService.getByIdForUser(100L, user)).thenReturn(taskA);
        when(repository.findFirstByUserAndEndAtIsNull(user)).thenReturn(Optional.of(existing));
        when(repository.save(any(TimeEntry.class))).thenAnswer(inv -> inv.getArgument(0));

        TimeEntry started = timerService.start(100L, user);

        assertThat(existing.isActive()).isFalse();
        assertThat(existing.getDurationSeconds()).isEqualTo(600L);
        assertThat(started.getTask()).isSameAs(taskA);
        verify(auditService).record(eq("TIMER_STOPPED"), eq("Task"), eq(101L), anyString());
        verify(auditService).record(eq("TIMER_STARTED"), eq("Task"), eq(100L), anyString());
    }

    @Test
    void stopActiveReturnsEmptyWhenNoActive() {
        when(repository.findFirstByUserAndEndAtIsNull(user)).thenReturn(Optional.empty());

        Optional<TimeEntry> stopped = timerService.stopActive(user, null);

        assertThat(stopped).isEmpty();
        verify(repository, never()).save(any(TimeEntry.class));
    }

    @Test
    void stopActiveCalculatesDuration() {
        TimeEntry existing = new TimeEntry(taskA, user, timerService.now().minusSeconds(125));
        when(repository.findFirstByUserAndEndAtIsNull(user)).thenReturn(Optional.of(existing));
        when(repository.save(any(TimeEntry.class))).thenAnswer(inv -> inv.getArgument(0));

        Optional<TimeEntry> stopped = timerService.stopActive(user, "done");

        assertThat(stopped).isPresent();
        assertThat(stopped.get().getDurationSeconds()).isEqualTo(125L);
        assertThat(stopped.get().getNote()).isEqualTo("done");
        assertThat(stopped.get().isActive()).isFalse();
    }

    @Test
    void totalSecondsForUserTodayIncludesActiveTimer() {
        // Active timer started 10 minutes before "now" (12:00 UTC). All within today.
        TimeEntry existing = new TimeEntry(taskA, user, timerService.now().minusMinutes(10));
        when(repository.findFirstByUserAndEndAtIsNull(user)).thenReturn(Optional.of(existing));
        // Two finished entries summing to 1800 seconds today.
        when(repository.sumDurationSecondsByUserBetween(eq(user), any(), any())).thenReturn(1800L);

        long total = timerService.totalSecondsForUserToday(user);

        assertThat(total).isEqualTo(1800L + 600L);
    }

    @Test
    void totalSecondsForTaskDelegates() {
        when(repository.sumDurationSecondsByTask(taskA)).thenReturn(123L);

        assertThat(timerService.totalSecondsForTask(taskA)).isEqualTo(123L);
    }

    @Test
    void formatDurationProducesReadableShortForms() {
        assertThat(TimerService.formatDuration(0L)).isEqualTo("0м");
        assertThat(TimerService.formatDuration(45L)).isEqualTo("0м");
        assertThat(TimerService.formatDuration(125L)).isEqualTo("2м");
        assertThat(TimerService.formatDuration(3600L)).isEqualTo("1ч");
        assertThat(TimerService.formatDuration(3725L)).isEqualTo("1ч 2м");
    }

    @Test
    void formatDurationLongIncludesSeconds() {
        assertThat(TimerService.formatDurationLong(0L)).isEqualTo("0м 0с");
        assertThat(TimerService.formatDurationLong(125L)).isEqualTo("2м 5с");
        assertThat(TimerService.formatDurationLong(3725L)).isEqualTo("1ч 2м 5с");
    }
}
