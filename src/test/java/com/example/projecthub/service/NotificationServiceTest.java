package com.example.projecthub.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.projecthub.entity.Notification;
import com.example.projecthub.entity.NotificationType;
import com.example.projecthub.entity.Role;
import com.example.projecthub.entity.User;
import com.example.projecthub.repository.NotificationRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    NotificationRepository repository;

    NotificationService service;

    User recipient;

    @BeforeEach
    void setUp() {
        Clock fixed = Clock.fixed(Instant.parse("2026-05-04T12:00:00Z"), ZoneOffset.UTC);
        service = new NotificationService(repository, fixed);
        recipient = new User("ivan", "x", Role.USER);
        recipient.setId(1L);
    }

    @Test
    void create_savesEntityWithFixedClock() {
        when(repository.save(any(Notification.class))).thenAnswer(inv -> inv.getArgument(0));

        Notification saved = service.create(recipient, NotificationType.TASK_ASSIGNED,
                "Назначена", "test", "/tasks/42");

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(repository).save(captor.capture());
        Notification toPersist = captor.getValue();
        assertThat(toPersist.getRecipient()).isEqualTo(recipient);
        assertThat(toPersist.getType()).isEqualTo(NotificationType.TASK_ASSIGNED);
        assertThat(toPersist.getTitle()).isEqualTo("Назначена");
        assertThat(toPersist.getMessage()).isEqualTo("test");
        assertThat(toPersist.getLink()).isEqualTo("/tasks/42");
        assertThat(toPersist.getCreatedAt()).isNotNull();
        assertThat(saved).isSameAs(toPersist);
    }

    @Test
    void unreadCount_delegatesToRepository() {
        when(repository.countByRecipientAndReadAtIsNull(recipient)).thenReturn(7L);
        assertThat(service.unreadCount(recipient)).isEqualTo(7L);
    }

    @Test
    void recent_returnsTop10() {
        Notification n = new Notification();
        when(repository.findTop10ByRecipientOrderByCreatedAtDesc(recipient)).thenReturn(List.of(n));
        assertThat(service.recent(recipient)).containsExactly(n);
    }

    @Test
    void page_returnsRepositoryResult() {
        Page<Notification> page = new PageImpl<>(List.of(new Notification()));
        when(repository.findAllByRecipientOrderByCreatedAtDesc(recipient, PageRequest.of(0, 20)))
                .thenReturn(page);
        Page<Notification> result = service.page(recipient, PageRequest.of(0, 20));
        assertThat(result).isSameAs(page);
    }

    @Test
    void markRead_setsReadAtForOwnNotification() {
        Notification n = new Notification(recipient, NotificationType.TASK_ASSIGNED,
                "t", "m", "/x", java.time.LocalDateTime.now());
        n.setId(11L);
        when(repository.findById(11L)).thenReturn(Optional.of(n));
        service.markRead(recipient, 11L);
        assertThat(n.getReadAt()).isNotNull();
    }

    @Test
    void markRead_ignoresOthersNotifications() {
        User other = new User("maria", "x", Role.USER);
        other.setId(2L);
        Notification n = new Notification(other, NotificationType.TASK_ASSIGNED,
                "t", "m", "/x", java.time.LocalDateTime.now());
        n.setId(11L);
        when(repository.findById(11L)).thenReturn(Optional.of(n));
        service.markRead(recipient, 11L);
        assertThat(n.getReadAt()).isNull();
    }

    @Test
    void markAllRead_returnsAffectedCount() {
        when(repository.markAllRead(recipient)).thenReturn(3);
        assertThat(service.markAllRead(recipient)).isEqualTo(3);
    }
}
