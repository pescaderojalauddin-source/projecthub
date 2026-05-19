package com.example.projecthub.service;

import com.example.projecthub.entity.Project;
import com.example.projecthub.entity.Task;
import com.example.projecthub.entity.TaskStatus;
import com.example.projecthub.repository.TaskRepository;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// модель календаря дедлайнов проекта (CSS-grid 7×N)
// в ячейке — день, список задач с дедлайном, бакет нагрузки
@Service
public class DeadlineCalendarService {

    private final TaskRepository taskRepository;

    public DeadlineCalendarService(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    @Transactional(readOnly = true)
    public Calendar build(Project project, YearMonth ym) {
        LocalDate firstOfMonth = ym.atDay(1);
        LocalDate lastOfMonth = ym.atEndOfMonth();

    // стартуем сетку с понедельника той недели, к к-рой принадлежит 1-е число
        LocalDate gridStart = firstOfMonth.with(DayOfWeek.MONDAY);
        if (gridStart.isAfter(firstOfMonth)) {
            gridStart = gridStart.minusWeeks(1);
        }
    // заканчиваем воскресеньем недели, к к-рой принадлежит последнее число
        LocalDate gridEnd = lastOfMonth.with(DayOfWeek.SUNDAY);
        if (gridEnd.isBefore(lastOfMonth)) {
            gridEnd = gridEnd.plusWeeks(1);
        }

        Map<LocalDate, List<Task>> byDay = new LinkedHashMap<>();
        for (Task t : taskRepository.findByProjectAndDeadlineBetween(project, gridStart, gridEnd)) {
            byDay.computeIfAbsent(t.getDeadline(), k -> new ArrayList<>()).add(t);
        }

        LocalDate today = LocalDate.now();
        List<Day> days = new ArrayList<>();
        for (LocalDate d = gridStart; !d.isAfter(gridEnd); d = d.plusDays(1)) {
            List<Task> tasks = byDay.getOrDefault(d, List.of());
            long openTasks = tasks.stream().filter(t -> t.getStatus() != TaskStatus.DONE).count();
            boolean inMonth = d.getMonth() == ym.getMonth();
            boolean overdue = d.isBefore(today) && openTasks > 0;
            String load = pickLoad(openTasks);
            days.add(new Day(d, inMonth, d.equals(today), overdue, tasks, load));
        }

        return new Calendar(ym, ym.minusMonths(1), ym.plusMonths(1), days);
    }

    private static String pickLoad(long openCount) {
        if (openCount == 0) return "none";
        if (openCount == 1) return "low";
        if (openCount <= 3) return "mid";
        return "high";
    }

    public record Day(
            LocalDate date,
            boolean inMonth,
            boolean today,
            boolean overdue,
            List<Task> tasks,
            String load) {
    }

    public record Calendar(YearMonth month, YearMonth prev, YearMonth next, List<Day> days) {
    }
}
