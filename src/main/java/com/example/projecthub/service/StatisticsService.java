package com.example.projecthub.service;

import com.example.projecthub.entity.TaskStatus;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// сервис сводной статистики для администратора: общее число проектов, задач, разбивка задач
@Service
@Transactional(readOnly = true)
public class StatisticsService {

    private static final Logger log = LoggerFactory.getLogger(StatisticsService.class);

    private final ProjectService projectService;
    private final TaskService taskService;
    private final UserService userService;

    private volatile Map<String, Object> cachedSnapshot = Map.of();

    public StatisticsService(ProjectService projectService, TaskService taskService, UserService userService) {
        this.projectService = projectService;
        this.taskService = taskService;
        this.userService = userService;
    }

    /**
     * асинхронно пересчитывает кеш сводной статистики
     * вызывается планировщиком и при необходимости — вручную
     * @return {@link CompletableFuture} с свежим снимком (см. {@link #snapshot()}).
     */
    @Async("applicationTaskExecutor")
    public CompletableFuture<Map<String, Object>> refreshAsync() {
        Map<String, Object> snap = computeSnapshot();
        cachedSnapshot = snap;
        log.debug("Сводная статистика обновлена: {}", snap);
        return CompletableFuture.completedFuture(snap);
    }

    // периодическое фоновое обновление кеша (раз в минуту)
    @Scheduled(fixedDelay = 60_000L, initialDelay = 5_000L)
    public void scheduledRefresh() {
        try {
            cachedSnapshot = computeSnapshot();
        } catch (Exception ex) {
            log.warn("Не удалось обновить кеш статистики: {}", ex.toString());
        }
    }

    // последний успешно посчитанный снимок (без обращения к БД)
    public Map<String, Object> getCachedSnapshot() {
        return cachedSnapshot;
    }

    // собирает снимок статистики: общие счётчики и разбивку задач по статусам результат
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, Object> snapshot() {
        return computeSnapshot();
    }

    private Map<String, Object> computeSnapshot() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("totalProjects", projectService.count());
        data.put("totalTasks", taskService.count());
        data.put("totalUsers", userService.findAll().size());

        Map<String, Long> tasksByStatus = new LinkedHashMap<>();
        for (TaskStatus s : TaskStatus.values()) {
            tasksByStatus.put(s.name(), taskService.countByStatus(s));
        }
        data.put("tasksByStatus", tasksByStatus);
        return data;
    }
}
