package com.example.projecthub.controller;

import com.example.projecthub.entity.Project;
import com.example.projecthub.entity.Task;
import com.example.projecthub.entity.User;
import com.example.projecthub.service.CurrentUserService;
import com.example.projecthub.service.ProjectService;
import com.example.projecthub.service.TaskService;
import com.opencsv.CSVWriter;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

// csv-экспорт задач проекта
// OpenCSV льёт в response-stream, без буферизации всего файла
@Controller
public class TaskExportController {

    private final TaskService taskService;
    private final ProjectService projectService;
    private final CurrentUserService currentUserService;

    public TaskExportController(TaskService taskService,
                                ProjectService projectService,
                                CurrentUserService currentUserService) {
        this.taskService = taskService;
        this.projectService = projectService;
        this.currentUserService = currentUserService;
    }

    @GetMapping("/projects/{projectId}/tasks/export.csv")
    public void exportCsv(@PathVariable Long projectId, HttpServletResponse response) throws IOException {
        User current = currentUserService.getCurrent();
        Project project = projectService.getByIdForUser(projectId, current);
        List<Task> tasks = taskService.findAllForProject(project);

        String safeTitle = sanitize(project.getTitle());
        String filename = "tasks-" + safeTitle + "-" + java.time.LocalDate.now() + ".csv";
        String encoded = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");

        response.setContentType("text/csv; charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Content-Disposition",
                "attachment; filename=\"tasks.csv\"; filename*=UTF-8''" + encoded);
    // bOM, чтобы Excel определил UTF-8 (без него ломаются заголовки на русском)
        response.getOutputStream().write(0xEF);
        response.getOutputStream().write(0xBB);
        response.getOutputStream().write(0xBF);

        try (Writer w = new OutputStreamWriter(response.getOutputStream(), StandardCharsets.UTF_8);
             CSVWriter csv = new CSVWriter(w, ';', CSVWriter.DEFAULT_QUOTE_CHARACTER,
                     CSVWriter.DEFAULT_ESCAPE_CHARACTER, "\r\n")) {
            csv.writeNext(new String[]{
                    "ID", "Название", "Описание", "Статус", "Приоритет",
                    "Дедлайн", "Исполнитель", "Теги", "Создано", "Обновлено"
            });
            for (Task t : tasks) {
                csv.writeNext(new String[]{
                        String.valueOf(t.getId()),
                        nullSafe(t.getTitle()),
                        nullSafe(t.getDescription()),
                        t.getStatus() != null ? t.getStatus().getLabel() : "",
                        t.getPriority() != null ? t.getPriority().getLabel() : "",
                        t.getDeadline() != null ? t.getDeadline().toString() : "",
                        (t.getAssignee() != null) ? t.getAssignee().getLogin() : "",
                        String.join(",", t.getTags()),
                        t.getCreatedAt() != null ? t.getCreatedAt().toString() : "",
                        t.getUpdatedAt() != null ? t.getUpdatedAt().toString() : ""
                });
            }
        }
    }

    private static String nullSafe(String s) {
        return s != null ? s : "";
    }

    // заменяет небезопасные для имени файла символы на дефис
    private static String sanitize(String s) {
        if (s == null) return "untitled";
        return s.replaceAll("[\\\\/:*?\"<>|\\s]+", "-");
    }
}
