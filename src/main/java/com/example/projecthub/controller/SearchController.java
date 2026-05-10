package com.example.projecthub.controller;

import com.example.projecthub.entity.Project;
import com.example.projecthub.entity.Role;
import com.example.projecthub.entity.Task;
import com.example.projecthub.entity.User;
import com.example.projecthub.repository.ProjectRepository;
import com.example.projecthub.repository.TaskRepository;
import com.example.projecthub.service.CurrentUserService;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Глобальный поиск из навбара.
 * <p>HTMX дёргает {@code GET /search?q=...} c {@code hx-target} на dropdown и
 * получает HTML-фрагмент с топ-5 совпавшими проектами и топ-8 задачами.
 * <p>USER видит только свои проекты (и задачи в них), ADMIN — всё.
 */
@Controller
public class SearchController {

    private static final int MAX_PROJECT_RESULTS = 5;
    private static final int MAX_TASK_RESULTS = 8;
    private static final int MIN_QUERY_LEN = 2;

    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;
    private final CurrentUserService currentUserService;

    public SearchController(ProjectRepository projectRepository,
                            TaskRepository taskRepository,
                            CurrentUserService currentUserService) {
        this.projectRepository = projectRepository;
        this.taskRepository = taskRepository;
        this.currentUserService = currentUserService;
    }

    /** HTMX-фрагмент с результатами поиска. */
    @GetMapping("/search")
    public String search(@RequestParam(value = "q", required = false) String q, Model model) {
        String query = q == null ? "" : q.trim();
        model.addAttribute("q", query);
        if (query.length() < MIN_QUERY_LEN) {
            model.addAttribute("projects", List.of());
            model.addAttribute("tasks", List.of());
            model.addAttribute("hint", "Введите минимум " + MIN_QUERY_LEN + " символа");
            return "fragments/search :: results";
        }

        User me = currentUserService.getCurrent();
        Pageable projectsPage = PageRequest.of(0, MAX_PROJECT_RESULTS);
        Pageable tasksPage = PageRequest.of(0, MAX_TASK_RESULTS);

        List<Project> projects;
        List<Task> tasks;
        if (me.getRole() == Role.ADMIN) {
            projects = projectRepository.searchByText(query, projectsPage);
            tasks = taskRepository.searchByText(query, tasksPage);
        } else {
            projects = projectRepository.searchByTextForOwner(query, me, projectsPage);
            tasks = taskRepository.searchByTextForOwner(query, me, tasksPage);
        }
        model.addAttribute("projects", projects);
        model.addAttribute("tasks", tasks);
        return "fragments/search :: results";
    }
}
