package com.example.projecthub.service;

import com.example.projecthub.entity.TaskStatus;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Сервис сводной статистики для администратора:
 * общее число проектов, задач, разбивка задач по статусам.
 */
@Service
@Transactional(readOnly = true)
public class StatisticsService {

    private final ProjectService projectService;
    private final TaskService taskService;
    private final UserService userService;

    public StatisticsService(ProjectService projectService, TaskService taskService, UserService userService) {
        this.projectService = projectService;
        this.taskService = taskService;
        this.userService = userService;
    }

    public Map<String, Object> snapshot() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("totalProjects", projectService.count());
        data.put("totalTasks", taskService.count());
        data.put("totalUsers", userService.findAll().size());

        Map<String, Long> tasksByStatus = new LinkedHashMap<>();
        for (TaskStatus s : TaskStatus.values()) {
            tasksByStatus.put(s.name(), taskService.countByStatus(s));
        }
        data.put("tasksByStatus", tasksByStatus);
        return data;
    }
}
