package com.example.projecthub.service;

import com.example.projecthub.entity.Project;
import com.example.projecthub.entity.Task;
import com.example.projecthub.entity.TaskStatus;
import com.example.projecthub.repository.TaskRepository;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Считает Burndown-серию для проекта: «сколько задач оставалось открытыми
 * (статус != DONE) на каждую дату за последние N дней».
 *
 * <p>Алгоритм:
 * <ul>
 *   <li>Грузим все задачи проекта одним запросом (createdAt, status, updatedAt).</li>
 *   <li>Для каждой даты d: open = задачи, у которых {@code createdAt.toLocalDate() <= d}
 *       и (статус != DONE ИЛИ {@code updatedAt.toLocalDate() > d}).</li>
 * </ul>
 * Это даёт точную картину «как уменьшался хвост работы день за днём» без
 * необходимости хранить snapshot-таблицу состояний — Envers тут не нужен.
 */
@Service
@Transactional(readOnly = true)
public class BurndownService {

    private static final int WINDOW_DAYS = 30;
    private static final DateTimeFormatter LABEL_FMT = DateTimeFormatter.ofPattern("dd.MM");

    private final TaskRepository taskRepository;

    public BurndownService(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    public Series forProject(Project project) {
        List<Task> tasks = taskRepository.findAllByProject(project);
        LocalDate today = LocalDate.now();
        LocalDate from = today.minusDays(WINDOW_DAYS - 1L);

        List<String> labels = new ArrayList<>(WINDOW_DAYS);
        List<Long> open = new ArrayList<>(WINDOW_DAYS);
        List<Long> done = new ArrayList<>(WINDOW_DAYS);

        for (int i = 0; i < WINDOW_DAYS; i++) {
            LocalDate d = from.plusDays(i);
            labels.add(d.format(LABEL_FMT));

            long openCount = 0;
            long doneCount = 0;
            for (Task t : tasks) {
                if (t.getCreatedAt() == null) continue;
                LocalDate created = t.getCreatedAt().toLocalDate();
                if (created.isAfter(d)) continue; // ещё не создан

                boolean isDoneByDate = t.getStatus() == TaskStatus.DONE
                        && t.getUpdatedAt() != null
                        && !t.getUpdatedAt().toLocalDate().isAfter(d);
                if (isDoneByDate) {
                    doneCount++;
                } else {
                    openCount++;
                }
            }
            open.add(openCount);
            done.add(doneCount);
        }
        return new Series(labels, open, done);
    }

    /** Сериализуемая структура для Thymeleaf/Chart.js. */
    public record Series(List<String> labels, List<Long> open, List<Long> done) {
    }
}
