package com.example.projecthub.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.projecthub.dto.TaskForm;
import com.example.projecthub.entity.Project;
import com.example.projecthub.entity.ProjectStatus;
import com.example.projecthub.entity.Role;
import com.example.projecthub.entity.Task;
import com.example.projecthub.entity.TaskStatus;
import com.example.projecthub.entity.User;
import com.example.projecthub.exception.AccessDeniedAppException;
import com.example.projecthub.repository.TaskRepository;
import com.example.projecthub.repository.UserRepository;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    TaskRepository taskRepository;

    @Mock
    UserRepository userRepository;

    @Mock
    ProjectService projectService;

    @InjectMocks
    TaskService taskService;

    private User owner;
    private User assignee;
    private User stranger;
    private Project project;
    private Task task;

    @BeforeEach
    void setUp() {
        owner = new User("ivan", "h", Role.USER);
        owner.setId(1L);
        assignee = new User("maria", "h", Role.USER);
        assignee.setId(2L);
        stranger = new User("oleg", "h", Role.USER);
        stranger.setId(3L);

        project = new Project("P", "d", ProjectStatus.ACTIVE, owner);
        project.setId(10L);

        task = new Task("T", "d", TaskStatus.TODO, LocalDate.now().plusDays(1), project, assignee);
        task.setId(100L);
    }

    @Test
    void createDelegatesAccessCheckToProjectService() {
        TaskForm f = new TaskForm();
        f.setTitle("New");
        f.setStatus(TaskStatus.TODO);
        f.setDeadline(LocalDate.now());
        f.setAssigneeId(2L);
        when(userRepository.findById(2L)).thenReturn(Optional.of(assignee));
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        Task saved = taskService.create(project, f, owner);

        assertThat(saved.getTitle()).isEqualTo("New");
        assertThat(saved.getAssignee()).isSameAs(assignee);
        verify(projectService).ensureAccessible(project, owner);
    }

    @Test
    void createWithoutAssignee() {
        TaskForm f = new TaskForm();
        f.setTitle("New");
        f.setStatus(TaskStatus.TODO);
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        Task saved = taskService.create(project, f, owner);

        assertThat(saved.getAssignee()).isNull();
        verify(userRepository, never()).findById(any());
    }

    @Test
    void changeStatusUpdatesStatus() {
        when(taskRepository.findById(100L)).thenReturn(Optional.of(task));
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        Task updated = taskService.changeStatus(100L, TaskStatus.DONE, owner);

        assertThat(updated.getStatus()).isEqualTo(TaskStatus.DONE);
    }

    @Test
    void assigneeCanAccessTask() {
        when(taskRepository.findById(100L)).thenReturn(Optional.of(task));

        Task t = taskService.getByIdForUser(100L, assignee);

        assertThat(t).isSameAs(task);
    }

    @Test
    void strangerCannotAccessTask() {
        when(taskRepository.findById(100L)).thenReturn(Optional.of(task));

        assertThatThrownBy(() -> taskService.getByIdForUser(100L, stranger))
                .isInstanceOf(AccessDeniedAppException.class);
    }
}
