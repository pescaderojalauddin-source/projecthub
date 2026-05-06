package com.example.projecthub.service;

import com.example.projecthub.entity.Project;
import com.example.projecthub.entity.Task;
import com.example.projecthub.entity.TimeEntry;
import com.example.projecthub.entity.User;
import com.example.projecthub.repository.TimeEntryRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Сервис учёта рабочего времени по задачам.
 *
 * <p>Основные правила:
 * <ul>
 *   <li>У пользователя одновременно может быть не больше одного активного таймера.</li>
 *   <li>Старт нового таймера автоматически останавливает предыдущий.</li>
 *   <li>Доступ к задаче проверяется так же, как в {@link TaskService} (владелец проекта,
 *       исполнитель или админ).</li>
 * </ul>
 */
@Service
@Transactional
public class TimerService {

    private final TimeEntryRepository repository;
    private final TaskService taskService;
    private final AuditService auditService;
    private final Clock clock;

    @org.springframework.beans.factory.annotation.Autowired
    public TimerService(TimeEntryRepository repository,
                        TaskService taskService,
                        AuditService auditService) {
        this(repository, taskService, auditService, Clock.systemDefaultZone());
    }

    /** Конструктор для тестов: позволяет подменить часы. */
    TimerService(TimeEntryRepository repository,
                 TaskService taskService,
                 AuditService auditService,
                 Clock clock) {
        this.repository = repository;
        this.taskService = taskService;
        this.auditService = auditService;
        this.clock = clock;
    }

    /**
     * Запустить таймер по задаче. Если уже есть активный — он будет остановлен (autoswitch).
     * Если пользователь нажал «Старт» по задаче, по которой у него уже идёт таймер, метод
     * вернёт текущую активную запись без изменений.
     */
    public TimeEntry start(Long taskId, User actor) {
        Task task = taskService.getByIdForUser(taskId, actor);
        Optional<TimeEntry> existing = repository.findFirstByUserAndEndAtIsNull(actor);
        if (existing.isPresent()) {
            TimeEntry active = existing.get();
            if (active.getTask().getId().equals(taskId)) {
                return active;
            }
            stopInternal(active, null);
        }
        TimeEntry entry = new TimeEntry(task, actor, LocalDateTime.now(clock));
        TimeEntry saved = repository.save(entry);
        auditService.record("TIMER_STARTED", "Task", task.getId(),
                "title=" + task.getTitle());
        return saved;
    }

    /**
     * Остановить активный таймер пользователя (если есть).
     * Возвращает остановленную запись или {@link Optional#empty()}, если активного не было.
     */
    public Optional<TimeEntry> stopActive(User actor, String note) {
        Optional<TimeEntry> existing = repository.findFirstByUserAndEndAtIsNull(actor);
        if (existing.isEmpty()) {
            return Optional.empty();
        }
        TimeEntry active = existing.get();
        // Доступ проверять не нужно: таймер принадлежит пользователю по построению.
        stopInternal(active, note);
        return Optional.of(active);
    }

    private void stopInternal(TimeEntry entry, String note) {
        entry.stop(LocalDateTime.now(clock), note);
        repository.save(entry);
        auditService.record("TIMER_STOPPED", "Task", entry.getTask().getId(),
                "duration=" + entry.getDurationSeconds() + "s");
    }

    @Transactional(readOnly = true)
    public Optional<TimeEntry> getActiveEntry(User user) {
        return repository.findFirstByUserAndEndAtIsNull(user);
    }

    /** История записей по задаче (для отображения под кнопкой «Старт/Стоп»). */
    @Transactional(readOnly = true)
    public List<TimeEntry> historyForTask(Task task, User actor) {
        taskService.ensureAccessible(task, actor);
        return repository.findAllByTaskOrderByStartAtDesc(task);
    }

    /** Постраничная история таймеров пользователя (для страницы профиля). */
    @Transactional(readOnly = true)
    public Page<TimeEntry> historyForUser(User user, Pageable pageable) {
        return repository.findAllByUserOrderByStartAtDesc(user, pageable);
    }

    @Transactional(readOnly = true)
    public long totalSecondsForTask(Task task) {
        return repository.sumDurationSecondsByTask(task);
    }

    /** Суммарное время пользователя за интервал [from; to). */
    @Transactional(readOnly = true)
    public long totalSecondsForUserBetween(User user, LocalDateTime from, LocalDateTime to) {
        return repository.sumDurationSecondsByUserBetween(user, from, to);
    }

    /** Суммарное время пользователя за сегодня. */
    @Transactional(readOnly = true)
    public long totalSecondsForUserToday(User user) {
        LocalDate today = LocalDate.now(clock);
        ZoneId zone = clock.getZone();
        LocalDateTime from = today.atStartOfDay();
        LocalDateTime to = today.plusDays(1).atStartOfDay();
        long fromActive = activeSecondsIfWithin(user, from, to, LocalDateTime.now(clock));
        return totalSecondsForUserBetween(user, from, to) + fromActive;
    }

    /** Суммарное время пользователя за последние 7 дней (включая сегодняшний). */
    @Transactional(readOnly = true)
    public long totalSecondsForUserLast7Days(User user) {
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime from = now.toLocalDate().minusDays(6).atStartOfDay();
        LocalDateTime to = now.toLocalDate().plusDays(1).atStartOfDay();
        long fromActive = activeSecondsIfWithin(user, from, to, now);
        return totalSecondsForUserBetween(user, from, to) + fromActive;
    }

    /** Карта taskId → суммарные секунды по проекту (только завершённые записи). */
    @Transactional(readOnly = true)
    public Map<Long, Long> totalsByProject(Project project) {
        Map<Long, Long> result = new HashMap<>();
        for (TimeEntryRepository.TaskTotal row : repository.sumDurationSecondsByProject(project.getId())) {
            result.put(row.getTaskId(), row.getTotal() != null ? row.getTotal() : 0L);
        }
        return result;
    }

    /**
     * Учитывает текущий незавершённый таймер пользователя, если он попадает в интервал [from; to).
     * Это даёт ощущение «живой» статистики на дашборде без ожидания нажатия «Стоп».
     */
    private long activeSecondsIfWithin(User user, LocalDateTime from, LocalDateTime to, LocalDateTime now) {
        Optional<TimeEntry> active = repository.findFirstByUserAndEndAtIsNull(user);
        if (active.isEmpty()) {
            return 0L;
        }
        LocalDateTime start = active.get().getStartAt();
        LocalDateTime cap = now.isBefore(to) ? now : to;
        LocalDateTime effectiveStart = start.isBefore(from) ? from : start;
        if (effectiveStart.isAfter(cap)) {
            return 0L;
        }
        return Math.max(0L, Duration.between(effectiveStart, cap).getSeconds());
    }

    /** Форматирует длительность в Hh Mm (или Mm для значений меньше часа). */
    public static String formatDuration(long seconds) {
        if (seconds <= 0) {
            return "0м";
        }
        long h = seconds / 3600;
        long m = (seconds % 3600) / 60;
        if (h == 0) {
            return m + "м";
        }
        if (m == 0) {
            return h + "ч";
        }
        return h + "ч " + m + "м";
    }

    /** Удобный вариант с форматированием для шаблонов. */
    public static String formatDurationLong(long seconds) {
        if (seconds <= 0) {
            return "0м 0с";
        }
        long h = seconds / 3600;
        long m = (seconds % 3600) / 60;
        long s = seconds % 60;
        StringBuilder sb = new StringBuilder();
        if (h > 0) sb.append(h).append("ч ");
        sb.append(m).append("м ").append(s).append("с");
        return sb.toString();
    }

    /** Используется в шаблонах через SpEL/utility bean. */
    public String format(long seconds) { return formatDuration(seconds); }
    public String formatLong(long seconds) { return formatDurationLong(seconds); }

    /** Текущее время по часам сервиса. Удобно для шаблонов и тестов. */
    public LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    /** Добивает начало текущего дня по часам сервиса. */
    public LocalDateTime todayStart() {
        return LocalDate.now(clock).atTime(LocalTime.MIDNIGHT);
    }
}
