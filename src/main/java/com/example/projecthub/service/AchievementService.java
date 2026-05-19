package com.example.projecthub.service;

import com.example.projecthub.entity.AchievementUnlocked;
import com.example.projecthub.entity.TaskStatus;
import com.example.projecthub.entity.User;
import com.example.projecthub.repository.AchievementUnlockedRepository;
import com.example.projecthub.repository.ProjectStarRepository;
import com.example.projecthub.repository.TaskRepository;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// подсчёт и разблокировка ачивок тек юзера
// критерии — простые sql-агрегации поверх задач/звёзд
@Service
public class AchievementService {

    private final TaskRepository taskRepository;
    private final ProjectStarRepository starRepository;
    private final AchievementUnlockedRepository unlockedRepository;

    public AchievementService(TaskRepository taskRepository,
                              ProjectStarRepository starRepository,
                              AchievementUnlockedRepository unlockedRepository) {
        this.taskRepository = taskRepository;
        this.starRepository = starRepository;
        this.unlockedRepository = unlockedRepository;
    }

    // каталог: все возможные ачивки с описанием и порогом. Порядок отображения сверху вниз
    public List<Achievement> catalog() {
        List<Achievement> list = new ArrayList<>();
        list.add(new Achievement("FIRST_DONE",        "Первая задача",   "Закрой первую задачу",                       "bi-flag-fill",        "bg-success"));
        list.add(new Achievement("TEN_DONE",          "Десятка",         "Закрой 10 задач",                            "bi-bookmark-check",   "bg-primary"));
        list.add(new Achievement("FIFTY_DONE",        "Полтинник",       "Закрой 50 задач",                            "bi-award-fill",       "bg-warning text-dark"));
        list.add(new Achievement("HUNDRED_DONE",      "Сотня",           "Закрой 100 задач",                           "bi-trophy-fill",      "bg-danger"));
        list.add(new Achievement("FIRST_STAR",        "Любимчик",        "Добавь первый проект в избранное",           "bi-star-fill",        "bg-info text-dark"));
        list.add(new Achievement("FIVE_STARS",        "Коллекционер",    "Пять избранных проектов",                    "bi-stars",            "bg-info text-dark"));
        list.add(new Achievement("NO_BLOCKERS",       "Чистый стол",     "0 заблокированных задач на тебе",            "bi-shield-check",     "bg-secondary"));
        list.add(new Achievement("BUSY_BEE",          "Жужжалка",        "5 задач в работе одновременно",              "bi-lightning-charge-fill", "bg-warning text-dark"));
        return list;
    }

    // свежий снимок прогресса: статусы каждой ачивки + новые разблокировки за этот вызов
    @Transactional
    public ProgressSnapshot evaluate(User user) {
        Map<TaskStatus, Long> byStatus = countByStatus(user);
        long doneCount = byStatus.getOrDefault(TaskStatus.DONE, 0L);
        long inProgress = byStatus.getOrDefault(TaskStatus.IN_PROGRESS, 0L);
        long blocked = byStatus.getOrDefault(TaskStatus.BLOCKED, 0L);
        long stars = starRepository.countByUser(user);
        long totalAssigned = byStatus.values().stream().mapToLong(Long::longValue).sum();

        Map<String, Boolean> matched = new LinkedHashMap<>();
        matched.put("FIRST_DONE",   doneCount >= 1);
        matched.put("TEN_DONE",     doneCount >= 10);
        matched.put("FIFTY_DONE",   doneCount >= 50);
        matched.put("HUNDRED_DONE", doneCount >= 100);
        matched.put("FIRST_STAR",   stars >= 1);
        matched.put("FIVE_STARS",   stars >= 5);
        matched.put("NO_BLOCKERS",  blocked == 0L && totalAssigned >= 5);
        matched.put("BUSY_BEE",     inProgress >= 5);

        Set<String> already = unlockedRepository.findCodesByUser(user);
        List<Achievement> newlyUnlocked = new ArrayList<>();
        for (Achievement a : catalog()) {
            if (Boolean.TRUE.equals(matched.get(a.code())) && !already.contains(a.code())) {
                unlockedRepository.save(new AchievementUnlocked(user, a.code()));
                newlyUnlocked.add(a);
            }
        }
        Set<String> unlockedCodes = new java.util.HashSet<>(already);
        newlyUnlocked.forEach(a -> unlockedCodes.add(a.code()));

        return new ProgressSnapshot(catalog(), unlockedCodes, newlyUnlocked,
                doneCount, inProgress, blocked, stars);
    }

    private Map<TaskStatus, Long> countByStatus(User user) {
        Map<TaskStatus, Long> out = new LinkedHashMap<>();
        for (Object[] row : taskRepository.countByAssigneeGroupByStatus(user)) {
            out.put((TaskStatus) row[0], ((Number) row[1]).longValue());
        }
        return out;
    }

    // описание ачивки
    public record Achievement(String code, String title, String description, String icon, String badgeClass) {
    }

    // снимок прогресса по ачивкам — что разблокировано, что — нет, и сколько счётчиков насчитали
    public record ProgressSnapshot(List<Achievement> all,
                                   Set<String> unlockedCodes,
                                   List<Achievement> newlyUnlocked,
                                   long doneCount,
                                   long inProgressCount,
                                   long blockedCount,
                                   long starsCount) {
    }
}
