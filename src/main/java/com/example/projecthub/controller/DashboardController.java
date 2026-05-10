package com.example.projecthub.controller;

import com.example.projecthub.entity.Project;
import com.example.projecthub.entity.Task;
import com.example.projecthub.entity.TaskStatus;
import com.example.projecthub.entity.User;
import com.example.projecthub.repository.TaskRepository;
import com.example.projecthub.service.CurrentUserService;
import com.example.projecthub.service.ProjectProgressService;
import com.example.projecthub.service.ProjectStarService;
import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * «Мой день» — дашборд залогиненного пользователя.
 * Четыре карточки: сегодняшние дедлайны, просроченные, в работе, к выполнению.
 * Также — графики (распределение по статусам, готово за 7 дней) и избранные проекты.
 */
@Controller
public class DashboardController {

    private static final List<TaskStatus> NOT_DONE = List.of(TaskStatus.DONE);
    private static final int CHART_DAYS = 7;

    private final TaskRepository taskRepository;
    private final CurrentUserService currentUserService;
    private final ProjectStarService starService;
    private final ProjectProgressService progressService;

    public DashboardController(TaskRepository taskRepository,
                               CurrentUserService currentUserService,
                               ProjectStarService starService,
                               ProjectProgressService progressService) {
        this.taskRepository = taskRepository;
        this.currentUserService = currentUserService;
        this.starService = starService;
        this.progressService = progressService;
    }

    /** Главная страница после логина. */
    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        User me = currentUserService.getCurrent();
        LocalDate today = LocalDate.now();

        List<Task> dueToday = taskRepository
                .findTop10ByAssigneeAndDeadlineAndStatusNotInOrderByDeadlineAsc(me, today, NOT_DONE);
        List<Task> overdue = taskRepository
                .findTop10ByAssigneeAndDeadlineBeforeAndStatusNotInOrderByDeadlineAsc(me, today, NOT_DONE);
        List<Task> inProgress = taskRepository
                .findTop10ByAssigneeAndStatusOrderByDeadlineAsc(me, TaskStatus.IN_PROGRESS);
        List<Task> todo = taskRepository
                .findTop10ByAssigneeAndStatusOrderByDeadlineAsc(me, TaskStatus.TODO);

        long countDueToday = taskRepository
                .countByAssigneeAndDeadlineAndStatusNotIn(me, today, NOT_DONE);
        long countOverdue = taskRepository
                .countByAssigneeAndDeadlineBeforeAndStatusNotIn(me, today, NOT_DONE);
        long countInProgress = taskRepository
                .countByAssigneeAndStatus(me, TaskStatus.IN_PROGRESS);
        long countTodo = taskRepository
                .countByAssigneeAndStatus(me, TaskStatus.TODO);
        long countDone = taskRepository
                .countByAssigneeAndStatus(me, TaskStatus.DONE);

        // Графики. Ключи — имена статусов (TODO/IN_PROGRESS/DONE/BLOCKED),
        // чтобы из шаблона можно было писать byStatus['TODO'] без EL→enum-конверсий.
        Map<String, Long> byStatus = new java.util.LinkedHashMap<>();
        for (TaskStatus s : TaskStatus.values()) {
            byStatus.put(s.name(), 0L);
        }
        for (Object[] row : taskRepository.countByAssigneeGroupByStatus(me)) {
            byStatus.put(((TaskStatus) row[0]).name(), ((Number) row[1]).longValue());
        }

        LocalDate from = today.minusDays(CHART_DAYS - 1L);
        LocalDateTime fromTs = from.atStartOfDay();
        Map<LocalDate, Long> doneByDay = new java.util.HashMap<>();
        for (Object[] row : taskRepository.countDoneByAssigneeSince(me, fromTs)) {
            // На H2 SELECT CAST(... AS date) возвращает java.sql.Date.
            LocalDate day = row[0] instanceof Date d ? d.toLocalDate() : (LocalDate) row[0];
            doneByDay.put(day, ((Number) row[1]).longValue());
        }
        List<String> chartLabels = new ArrayList<>(CHART_DAYS);
        List<Long> chartData = new ArrayList<>(CHART_DAYS);
        for (int i = 0; i < CHART_DAYS; i++) {
            LocalDate d = from.plusDays(i);
            chartLabels.add(String.format("%02d.%02d", d.getDayOfMonth(), d.getMonthValue()));
            chartData.add(doneByDay.getOrDefault(d, 0L));
        }

        // Избранные проекты (топ-5) + их прогресс
        List<Project> favourites = starService.listFavourites(me);
        List<Project> favouritesTop = favourites.size() > 5 ? favourites.subList(0, 5) : favourites;
        Map<Long, ProjectProgressService.Progress> favProgress = progressService
                .forProjects(favouritesTop.stream().map(Project::getId).toList());

        model.addAttribute("me", me);
        model.addAttribute("today", today);
        model.addAttribute("dueToday", dueToday);
        model.addAttribute("overdue", overdue);
        model.addAttribute("inProgress", inProgress);
        model.addAttribute("todo", todo);
        model.addAttribute("countDueToday", countDueToday);
        model.addAttribute("countOverdue", countOverdue);
        model.addAttribute("countInProgress", countInProgress);
        model.addAttribute("countTodo", countTodo);
        model.addAttribute("countDone", countDone);
        model.addAttribute("byStatus", byStatus);
        model.addAttribute("chartLabels", chartLabels);
        model.addAttribute("chartData", chartData);
        model.addAttribute("favourites", favouritesTop);
        model.addAttribute("favouritesProgress", favProgress);
        model.addAttribute("favouritesTotal", favourites.size());
        return "dashboard";
    }
}
