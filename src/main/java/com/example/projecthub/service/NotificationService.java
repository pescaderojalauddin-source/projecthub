package com.example.projecthub.service;

import com.example.projecthub.entity.Notification;
import com.example.projecthub.entity.NotificationType;
import com.example.projecthub.entity.User;
import com.example.projecthub.repository.NotificationRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Сервис уведомлений.
 *
 * <p>Создаёт записи в таблице {@code notifications}, отдаёт ленту и счётчик
 * непрочитанных, поддерживает пометку прочитанным. Сами уведомления порождаются
 * асинхронно через {@link NotificationListener}.</p>
 */
@Service
@Transactional
public class NotificationService {

    private final NotificationRepository repository;
    private final Clock clock;

    @Autowired
    public NotificationService(NotificationRepository repository) {
        this(repository, Clock.systemDefaultZone());
    }

    NotificationService(NotificationRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    /** Создать новое уведомление. */
    public Notification create(User recipient, NotificationType type, String title,
                               String message, String link) {
        Notification n = new Notification(recipient, type, title, message, link, LocalDateTime.now(clock));
        return repository.save(n);
    }

    /** Количество непрочитанных уведомлений у пользователя. */
    @Transactional(readOnly = true)
    public long unreadCount(User user) {
        return repository.countByRecipientAndReadAtIsNull(user);
    }

    /** Последние 10 уведомлений (для выпадающего списка в навбаре). */
    @Transactional(readOnly = true)
    public List<Notification> recent(User user) {
        return repository.findTop10ByRecipientOrderByCreatedAtDesc(user);
    }

    /** Лента уведомлений (страница) — для отдельной страницы /notifications. */
    @Transactional(readOnly = true)
    public Page<Notification> page(User user, Pageable pageable) {
        return repository.findAllByRecipientOrderByCreatedAtDesc(user, pageable);
    }

    /** Пометить одно уведомление прочитанным. */
    public void markRead(User user, Long id) {
        repository.findById(id)
                .filter(n -> n.getRecipient().getId().equals(user.getId()))
                .ifPresent(n -> n.markRead(LocalDateTime.now(clock)));
    }

    /** Пометить все уведомления пользователя прочитанными. Возвращает количество затронутых. */
    public int markAllRead(User user) {
        return repository.markAllRead(user);
    }
}
