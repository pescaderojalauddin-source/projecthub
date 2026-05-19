package com.example.projecthub.service;

import com.example.projecthub.entity.TaskStatus;
import com.example.projecthub.repository.TaskRepository;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

// считает прогресс проекта (всего задач / готовых задач / процент) пачкой, одним
@Service
public class ProjectProgressService {

    private final TaskRepository taskRepository;

    public ProjectProgressService(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    // снимок прогресса по одному проекту
    public record Progress(long total, long done, int percent) {
        public static Progress empty() {
            return new Progress(0L, 0L, 0);
        }
    }

    // считает прогресс одним SQL-запросом для всех переданных id
    public Map<Long, Progress> forProjects(Collection<Long> projectIds) {
        Map<Long, Progress> result = new HashMap<>();
        if (projectIds == null || projectIds.isEmpty()) {
            return result;
        }
        Map<Long, long[]> bucket = new HashMap<>(); // [total, done]
        List<Object[]> rows = taskRepository.countByProjectIdGroupByStatus(projectIds);
        for (Object[] row : rows) {
            Long projectId = (Long) row[0];
            TaskStatus status = (TaskStatus) row[1];
            long count = ((Number) row[2]).longValue();
            long[] counters = bucket.computeIfAbsent(projectId, k -> new long[2]);
            counters[0] += count;
            if (status == TaskStatus.DONE) {
                counters[1] += count;
            }
        }
        for (Long id : projectIds) {
            long[] counters = bucket.get(id);
            if (counters == null || counters[0] == 0) {
                result.put(id, Progress.empty());
            } else {
                int percent = (int) Math.round(100.0 * counters[1] / counters[0]);
                result.put(id, new Progress(counters[0], counters[1], percent));
            }
        }
        return result;
    }
}
