package com.example.projecthub.controller;

import com.example.projecthub.entity.Task;
import com.example.projecthub.entity.TaskStatus;
import com.example.projecthub.entity.User;
import com.example.projecthub.repository.TaskRepository;
import com.example.projecthub.service.CurrentUserService;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * «Мой день» — дашборд залогиненного пользователя.
 * Четыре карточки: сегодняшние дедлайны, просроченные, в работе, к выполнению.
 */
@Controller
public class DashboardController {

    private static final List<TaskStatus> NOT_DONE = List.of(TaskStatus.DONE);

    private final TaskRepository taskRepository;
    private final CurrentUserService currentUserService;

    public DashboardController(TaskRepository taskRepository,
                               CurrentUserService currentUserService) {
        this.taskRepository = taskRepository;
        this.currentUserService = currentUserService;
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
        return "dashboard";
    }
}
