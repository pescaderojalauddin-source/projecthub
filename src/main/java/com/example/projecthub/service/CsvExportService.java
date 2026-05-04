package com.example.projecthub.service;

import com.example.projecthub.entity.Project;
import com.example.projecthub.entity.Task;
import com.example.projecthub.entity.User;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
 * <p>Тяжёлая часть (формирование строк) выполняется как {@code @Async}-метод, возвращающий
 * {@link CompletableFuture}. На вход поступают plain-record-ы {@link TaskRow}, заранее
 * собранные на потоке HTTP-запроса — поэтому асинхронный поток уже не имеет дела с
 * lazy-ассоциациями Hibernate.</p>
 */
@Service
public class CsvExportService {

    private static final Logger log = LoggerFactory.getLogger(CsvExportService.class);
    private static final DateTimeFormatter DEADLINE_FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    /**
     * Снимок строки задачи для экспорта — без зависимости от Hibernate-сессии.
     */
    public record TaskRow(Long id, String title, String status, LocalDate deadline,
                          String assigneeLogin, LocalDateTime createdAt) {
        public static TaskRow from(Task t) {
            User a = t.getAssignee();
            return new TaskRow(
                    t.getId(),
                    t.getTitle(),
                    t.getStatus() != null ? t.getStatus().name() : "",
                    t.getDeadline(),
                    a != null ? a.getLogin() : null,
                    t.getCreatedAt());
        }
    }

    /**
     * Сформировать CSV-байты по списку задач проекта.
     *
     * <p>Используется UTF-8 BOM, чтобы Excel корректно определял кодировку.</p>
     */
    @Async
    public CompletableFuture<byte[]> exportProjectTasks(Long projectId, List<TaskRow> rows) {
        StringBuilder sb = new StringBuilder(256 + rows.size() * 64);
        sb.append('\uFEFF'); // UTF-8 BOM для Excel
        sb.append("id;title;status;deadline;assignee;created_at\r\n");
        for (TaskRow r : rows) {
            sb.append(r.id()).append(';');
            sb.append(escape(r.title())).append(';');
            sb.append(r.status()).append(';');
            sb.append(r.deadline() != null ? DEADLINE_FMT.format(r.deadline()) : "").append(';');
            sb.append(r.assigneeLogin() != null ? escape(r.assigneeLogin()) : "").append(';');
            sb.append(r.createdAt() != null ? r.createdAt() : "").append("\r\n");
        }
        log.info("CSV export: project={} rows={}", projectId, rows.size());
        return CompletableFuture.completedFuture(
                sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    /** Совместимость со старой сигнатурой: {@link Project} + {@link Task} → {@link TaskRow}. */
    public CompletableFuture<byte[]> exportProjectTasks(Project project, List<Task> tasks) {
        List<TaskRow> rows = tasks.stream().map(TaskRow::from).toList();
        return exportProjectTasks(project.getId(), rows);
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
