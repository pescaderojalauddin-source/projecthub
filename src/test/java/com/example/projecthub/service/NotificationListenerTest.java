package com.example.projecthub.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.example.projecthub.entity.NotificationType;
import com.example.projecthub.entity.Project;
import com.example.projecthub.entity.ProjectStatus;
import com.example.projecthub.entity.Role;
import com.example.projecthub.entity.TaskStatus;
import com.example.projecthub.entity.User;
import com.example.projecthub.event.CommentAddedEvent;
import com.example.projecthub.event.TaskAssignedEvent;
import com.example.projecthub.event.TaskStatusChangedEvent;
import com.example.projecthub.repository.ProjectRepository;
import com.example.projecthub.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationListenerTest {

    @Mock
    NotificationService notificationService;

    @Mock
    UserRepository userRepository;

    @Mock
    ProjectRepository projectRepository;

    @InjectMocks
    NotificationListener listener;

    User actor;
    User assignee;
    User owner;
    Project project;

    @BeforeEach
    void setUp() {
        actor = userOf(1L, "ivan");
        assignee = userOf(2L, "maria");
        owner = userOf(3L, "admin");
        project = new Project("DB", "", ProjectStatus.ACTIVE, owner);
        project.setId(10L);
    }

    @Test
    void onTaskAssigned_notifiesAssignee() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(assignee));
        listener.onTaskAssigned(new TaskAssignedEvent(100L, "T", 2L, "ivan"));
        verify(notificationService).create(eq(assignee), eq(NotificationType.TASK_ASSIGNED),
                any(), eq("T"), eq("/tasks/100"));
    }

    @Test
    void onTaskAssigned_skipsSelfAssignment() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(actor));
        listener.onTaskAssigned(new TaskAssignedEvent(100L, "T", 1L, "ivan"));
        verifyNoInteractions(notificationService);
    }

    @Test
    void onTaskAssigned_skipsWhenAssigneeMissing() {
        listener.onTaskAssigned(new TaskAssignedEvent(100L, "T", null, "ivan"));
        verifyNoInteractions(notificationService);
    }

    @Test
    void onTaskStatusChanged_notifiesAssigneeAndOwner() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(assignee));
        when(projectRepository.findById(10L)).thenReturn(Optional.of(project));

        listener.onTaskStatusChanged(new TaskStatusChangedEvent(100L, "T", 10L, 2L,
                TaskStatus.TODO, TaskStatus.IN_PROGRESS, "ivan"));

        verify(notificationService).create(eq(assignee),
                eq(NotificationType.TASK_STATUS_CHANGED), any(), any(), eq("/tasks/100"));
        verify(notificationService).create(eq(owner),
                eq(NotificationType.TASK_STATUS_CHANGED), any(), any(), eq("/tasks/100"));
    }

    @Test
    void onTaskStatusChanged_doesNotDuplicateWhenAssigneeIsOwner() {
        Project ownProject = new Project("solo", "", ProjectStatus.ACTIVE, assignee);
        ownProject.setId(11L);
        when(userRepository.findById(2L)).thenReturn(Optional.of(assignee));
        when(projectRepository.findById(11L)).thenReturn(Optional.of(ownProject));

        listener.onTaskStatusChanged(new TaskStatusChangedEvent(100L, "T", 11L, 2L,
                TaskStatus.TODO, TaskStatus.IN_PROGRESS, "ivan"));

        ArgumentCaptor<User> users = ArgumentCaptor.forClass(User.class);
        verify(notificationService, org.mockito.Mockito.times(1))
                .create(users.capture(), eq(NotificationType.TASK_STATUS_CHANGED),
                        any(), any(), any());
        assertThat(users.getValue().getLogin()).isEqualTo("maria");
    }

    @Test
    void onCommentAdded_notifiesAssigneeAndOwnerExceptAuthor() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(assignee));
        when(projectRepository.findById(10L)).thenReturn(Optional.of(project));

        listener.onCommentAdded(new CommentAddedEvent(100L, "T", 10L, 2L, 1L, "ivan", "preview"));

        verify(notificationService).create(eq(assignee),
                eq(NotificationType.COMMENT_ADDED), any(), eq("preview"), eq("/tasks/100"));
        verify(notificationService).create(eq(owner),
                eq(NotificationType.COMMENT_ADDED), any(), eq("preview"), eq("/tasks/100"));
    }

    @Test
    void onCommentAdded_skipsAuthor() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(assignee));
        Project byMaria = new Project("p", "", ProjectStatus.ACTIVE, assignee);
        byMaria.setId(11L);
        when(projectRepository.findById(11L)).thenReturn(Optional.of(byMaria));

        // Author is maria (assignee+owner), so no notifications
        listener.onCommentAdded(new CommentAddedEvent(100L, "T", 11L, 2L, 2L, "maria", "preview"));
        verify(notificationService, never()).create(any(), any(), any(), any(), any());
    }

    private static User userOf(long id, String login) {
        User u = new User(login, "x", Role.USER);
        u.setId(id);
        return u;
    }
}
