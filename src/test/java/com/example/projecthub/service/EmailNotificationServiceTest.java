package com.example.projecthub.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.projecthub.entity.Project;
import com.example.projecthub.entity.ProjectStatus;
import com.example.projecthub.entity.Role;
import com.example.projecthub.entity.Task;
import com.example.projecthub.entity.TaskStatus;
import com.example.projecthub.entity.User;
import com.example.projecthub.repository.TaskRepository;
import com.example.projecthub.repository.UserRepository;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

// тестим логику дайджеста — без реального SMTP. mailSender отсутствует — dry-run в лог
@ExtendWith(MockitoExtension.class)
class EmailNotificationServiceTest {

    @Mock UserRepository userRepository;
    @Mock TaskRepository taskRepository;
    @Mock ObjectProvider<JavaMailSender> mailSenderProvider;
    @Mock JavaMailSender mailSender;

    User ivan;
    User maria;
    User mute;
    Project project;

    @BeforeEach
    void setUp() {
        ivan = new User("ivan", "h", Role.USER);
        ivan.setId(1L);
        ivan.setEmail("ivan@example.com");
        ivan.setEmailNotifications(true);

        maria = new User("maria", "h", Role.USER);
        maria.setId(2L);
        maria.setEmail("maria@example.com");
        maria.setEmailNotifications(true);

    // muteUser отписан — не должен получать
        mute = new User("mute", "h", Role.USER);
        mute.setId(3L);
        mute.setEmail("mute@example.com");
        mute.setEmailNotifications(false);

        project = new Project("P", "d", ProjectStatus.ACTIVE, ivan);
        project.setId(10L);
    }

    private Task overdueTaskFor(User u, long id) {
        Task t = new Task("overdue", "x", TaskStatus.IN_PROGRESS,
                LocalDate.now().minusDays(3), project, u);
        t.setId(id);
        return t;
    }

    @Test
    void disabledFlagSkipsExecution() {
        EmailNotificationService svc = new EmailNotificationService(
                userRepository, taskRepository, mailSenderProvider, false, "from@x");

        svc.sendDailyDigest();

        verify(userRepository, never()).findAll();
    }

    @Test
    void dryRunModeReportsCountWithoutSendingMail() {
    // sender отсутствует — должно работать в dry-run режиме
        when(mailSenderProvider.getIfAvailable()).thenReturn(null);
        when(userRepository.findAll()).thenReturn(List.of(ivan, maria, mute));
        when(taskRepository.findTop10ByAssigneeAndDeadlineBeforeAndStatusNotInOrderByDeadlineAsc(
                any(), any(), anyCollection()))
                .thenAnswer(inv -> {
                    User u = inv.getArgument(0);
                    if (u == ivan) return List.of(overdueTaskFor(ivan, 1L));
                    if (u == maria) return List.of(overdueTaskFor(maria, 2L), overdueTaskFor(maria, 3L));
                    return List.of();
                });

        EmailNotificationService svc = new EmailNotificationService(
                userRepository, taskRepository, mailSenderProvider, true, "from@x");

        int sent = svc.runOnce();

    // 2 юзера c просроченными задачами получили dry-run, mute не получил
        assertThat(sent).isEqualTo(2);
    }

    @Test
    void usersWithoutOverdueTasksAreSkipped() {
        when(mailSenderProvider.getIfAvailable()).thenReturn(null);
        when(userRepository.findAll()).thenReturn(List.of(ivan));
        when(taskRepository.findTop10ByAssigneeAndDeadlineBeforeAndStatusNotInOrderByDeadlineAsc(
                any(), any(), anyCollection()))
                .thenReturn(List.of());

        EmailNotificationService svc = new EmailNotificationService(
                userRepository, taskRepository, mailSenderProvider, true, "from@x");

        int sent = svc.runOnce();

        assertThat(sent).isZero();
    }

    @Test
    void unsubscribedUserIsSkippedEvenWithOverdue() {
        when(mailSenderProvider.getIfAvailable()).thenReturn(null);
        when(userRepository.findAll()).thenReturn(List.of(mute));
    // mute отписан — не должно вызвать репо за задачами вовсе

        EmailNotificationService svc = new EmailNotificationService(
                userRepository, taskRepository, mailSenderProvider, true, "from@x");

        int sent = svc.runOnce();

        assertThat(sent).isZero();
        verify(taskRepository, never())
                .findTop10ByAssigneeAndDeadlineBeforeAndStatusNotInOrderByDeadlineAsc(
                        any(), any(), anyCollection());
    }

    @Test
    void usersWithoutEmailAreSkipped() {
        ivan.setEmail(null);
        when(mailSenderProvider.getIfAvailable()).thenReturn(null);
        when(userRepository.findAll()).thenReturn(List.of(ivan));

        EmailNotificationService svc = new EmailNotificationService(
                userRepository, taskRepository, mailSenderProvider, true, "from@x");

        int sent = svc.runOnce();

        assertThat(sent).isZero();
    }

    @Test
    void realSenderReceivesMessage() {
    // если sender есть — он должен получить SimpleMailMessage
        when(mailSenderProvider.getIfAvailable()).thenReturn(mailSender);
        when(userRepository.findAll()).thenReturn(List.of(ivan));
        when(taskRepository.findTop10ByAssigneeAndDeadlineBeforeAndStatusNotInOrderByDeadlineAsc(
                any(), any(), anyCollection()))
                .thenReturn(List.of(overdueTaskFor(ivan, 1L)));

        EmailNotificationService svc = new EmailNotificationService(
                userRepository, taskRepository, mailSenderProvider, true, "from@example.com");

        int sent = svc.runOnce();

        assertThat(sent).isEqualTo(1);
        verify(mailSender).send(any(SimpleMailMessage.class));
    }
}
