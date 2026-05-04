package com.example.projecthub.service;

import com.example.projecthub.entity.Project;
import com.example.projecthub.entity.Task;
import com.example.projecthub.entity.User;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Сервис экспорта данных в CSV.
 *
 * <p>Тяжёлая часть (формирование строк) сделана как {@code @Async}-метод, возвращающий
 * {@link CompletableFuture}, чтобы при росте объёмов её можно было выполнять в фоновом пуле,
 * не блокируя HTTP-поток. Сейчас данные приходят страницами в памяти, но абстракция уже готова
 * к стримингу из БД.</p>
 */
@Service
public class CsvExportService {

    private static final Logger log = LoggerFactory.getLogger(CsvExportService.class);
    private static final DateTimeFormatter DEADLINE_FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    /**
     * Сформировать CSV-байты по списку задач проекта.
     *
     * <p>Используется UTF-8 BOM, чтобы Excel корректно определял кодировку.</p>
     */
    @Async
    public CompletableFuture<byte[]> exportProjectTasks(Project project, List<Task> tasks) {
        StringBuilder sb = new StringBuilder(256 + tasks.size() * 64);
        sb.append('\uFEFF'); // UTF-8 BOM для Excel
        sb.append("id;title;status;deadline;assignee;created_at\r\n");
        for (Task t : tasks) {
            sb.append(t.getId()).append(';');
            sb.append(escape(t.getTitle())).append(';');
            sb.append(t.getStatus()).append(';');
            sb.append(t.getDeadline() != null ? DEADLINE_FMT.format(t.getDeadline()) : "").append(';');
            User assignee = t.getAssignee();
            sb.append(assignee != null ? escape(assignee.getLogin()) : "").append(';');
            sb.append(t.getCreatedAt() != null ? t.getCreatedAt() : "").append("\r\n");
        }
        log.info("CSV export: project={} rows={}", project.getId(), tasks.size());
        return CompletableFuture.completedFuture(sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    private static String escape(String s) {
        if (s == null) {
            return "";
        }
        boolean needsQuoting = s.indexOf(';') >= 0 || s.indexOf('"') >= 0
                || s.indexOf('\n') >= 0 || s.indexOf('\r') >= 0;
        String value = s.replace("\"", "\"\"");
        return needsQuoting ? "\"" + value + "\"" : value;
    }
}
