package com.example.projecthub.service;

import com.example.projecthub.entity.AuditLog;
import com.example.projecthub.entity.Task;
import com.example.projecthub.entity.TimeEntry;
import com.example.projecthub.entity.User;
import com.example.projecthub.repository.AuditLogRepository;
import com.example.projecthub.repository.TaskRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Агрегатор данных для главной страницы «Сегодня».
 * Отдельный сервис, чтобы не плодить логику по контроллеру.
 */
@Service
@Transactional(readOnly = true)
public class DashboardService {

    private final TaskRepository taskRepository;
    private final AuditLogRepository auditLogRepository;
    private final TimerService timerService;
    private final Clock clock;

    @org.springframework.beans.factory.annotation.Autowired
    public DashboardService(TaskRepository taskRepository,
                            AuditLogRepository auditLogRepository,
                            TimerService timerService) {
        this(taskRepository, auditLogRepository, timerService, Clock.systemDefaultZone());
    }

    DashboardService(TaskRepository taskRepository,
                     AuditLogRepository auditLogRepository,
                     TimerService timerService,
                     Clock clock) {
        this.taskRepository = taskRepository;
        this.auditLogRepository = auditLogRepository;
        this.timerService = timerService;
        this.clock = clock;
    }

    public DashboardData snapshot(User user) {
        LocalDate today = LocalDate.now(clock);
        List<Task> overdue = taskRepository.findOverdueForUser(user, today);
        List<Task> upcoming = taskRepository.findUpcomingForUser(user, today, today.plusDays(7));
        List<Task> openTasks = taskRepository.findOpenForUser(user);
        long secondsToday = timerService.totalSecondsForUserToday(user);
        long secondsWeek = timerService.totalSecondsForUserLast7Days(user);
        Optional<TimeEntry> active = timerService.getActiveEntry(user);
        List<AuditLog> recent = auditLogRepository
                .findAllByActorOrderByCreatedAtDesc(user.getLogin(), PageRequest.of(0, 8))
                .getContent();
        return new DashboardData(today, overdue, upcoming, openTasks, secondsToday, secondsWeek, active, recent);
    }

    public static final class DashboardData {
        private final LocalDate today;
        private final List<Task> overdue;
        private final List<Task> upcomingWeek;
        private final List<Task> openTasks;
        private final long secondsToday;
        private final long secondsLast7Days;
        private final Optional<TimeEntry> activeEntry;
        private final List<AuditLog> recentAudit;

        public DashboardData(LocalDate today, List<Task> overdue, List<Task> upcomingWeek,
                             List<Task> openTasks, long secondsToday, long secondsLast7Days,
                             Optional<TimeEntry> activeEntry, List<AuditLog> recentAudit) {
            this.today = today;
            this.overdue = overdue;
            this.upcomingWeek = upcomingWeek;
            this.openTasks = openTasks;
            this.secondsToday = secondsToday;
            this.secondsLast7Days = secondsLast7Days;
            this.activeEntry = activeEntry;
            this.recentAudit = recentAudit;
        }

        public LocalDate getToday() { return today; }
        public List<Task> getOverdue() { return overdue; }
        public List<Task> getUpcomingWeek() { return upcomingWeek; }
        public List<Task> getOpenTasks() { return openTasks; }
        public long getSecondsToday() { return secondsToday; }
        public long getSecondsLast7Days() { return secondsLast7Days; }
        public Optional<TimeEntry> getActiveEntry() { return activeEntry; }
        public List<AuditLog> getRecentAudit() { return recentAudit; }
        public int getOverdueCount() { return overdue.size(); }
        public int getUpcomingWeekCount() { return upcomingWeek.size(); }
        public int getOpenTaskCount() { return openTasks.size(); }
    }
}
