package com.example.projecthub.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.example.projecthub.entity.Project;
import com.example.projecthub.entity.ProjectStatus;
import com.example.projecthub.entity.Role;
import com.example.projecthub.entity.Task;
import com.example.projecthub.entity.TaskStatus;
import com.example.projecthub.entity.User;
import com.example.projecthub.repository.TaskRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

// тесты burndown — ставим createdAt руками, чтоб симулировать «давно созданная задача»
@ExtendWith(MockitoExtension.class)
class BurndownServiceTest {

    @Mock TaskRepository taskRepository;

    BurndownService svc;
    Project project;
    User owner;

    @BeforeEach
    void setUp() {
        owner = new User("ivan", "h", Role.USER);
        owner.setId(1L);
        project = new Project("P", "d", ProjectStatus.ACTIVE, owner);
        project.setId(10L);
        svc = new BurndownService(taskRepository);
    }

    private Task taskCreatedDaysAgo(int days, TaskStatus status) {
        Task t = new Task("t", "d", status, null, project, owner);
        t.setCreatedAt(LocalDateTime.now().minusDays(days));
        return t;
    }

    @Test
    void seriesHas30PointsForWindow() {
        when(taskRepository.findAllByProject(project)).thenReturn(List.of());
        BurndownService.Series s = svc.forProject(project);
        assertThat(s.labels()).hasSize(30);
        assertThat(s.open()).hasSize(30);
        assertThat(s.done()).hasSize(30);
    }

    @Test
    void emptyProjectGivesAllZeros() {
        when(taskRepository.findAllByProject(project)).thenReturn(List.of());
        BurndownService.Series s = svc.forProject(project);
        assertThat(s.open()).allMatch(v -> v == 0L);
        assertThat(s.done()).allMatch(v -> v == 0L);
    }

    @Test
    void openTaskCountsAsOpenOnLastDay() {
        when(taskRepository.findAllByProject(project)).thenReturn(List.of(
                taskCreatedDaysAgo(0, TaskStatus.TODO) // создана сегодня, открыта
        ));
        BurndownService.Series s = svc.forProject(project);
    // последний элемент = сегодня
        assertThat(s.open().get(29)).isEqualTo(1L);
        assertThat(s.done().get(29)).isEqualTo(0L);
    }

    @Test
    void taskCreatedTodayDoesNotAppearOnEarlierDays() {
    // задача создана сегодня — на «вчерашний» день её ещё нет
        when(taskRepository.findAllByProject(project)).thenReturn(List.of(
                taskCreatedDaysAgo(0, TaskStatus.TODO)
        ));
        BurndownService.Series s = svc.forProject(project);
        assertThat(s.open().get(0)).isZero();
        assertThat(s.open().get(28)).isZero();
        assertThat(s.open().get(29)).isEqualTo(1L);
    }

    @Test
    void multipleTasksAreCounted() {
        when(taskRepository.findAllByProject(project)).thenReturn(List.of(
                taskCreatedDaysAgo(10, TaskStatus.TODO),
                taskCreatedDaysAgo(5, TaskStatus.IN_PROGRESS),
                taskCreatedDaysAgo(1, TaskStatus.BLOCKED)
        ));
        BurndownService.Series s = svc.forProject(project);
    // на «сегодня» — все 3 открыты
        assertThat(s.open().get(29)).isEqualTo(3L);
    }
}
