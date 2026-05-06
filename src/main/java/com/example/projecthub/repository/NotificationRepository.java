package com.example.projecthub.repository;

import com.example.projecthub.entity.Notification;
import com.example.projecthub.entity.User;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Репозиторий уведомлений.
 */
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /** Лента уведомлений пользователя (новые сверху). */
    Page<Notification> findAllByRecipientOrderByCreatedAtDesc(User recipient, Pageable pageable);

    /** Последние N уведомлений — для выпадающего списка в навбаре. */
    List<Notification> findTop10ByRecipientOrderByCreatedAtDesc(User recipient);

    /** Количество непрочитанных уведомлений. */
    long countByRecipientAndReadAtIsNull(User recipient);

    /** Массово пометить все уведомления пользователя прочитанными. */
    @Modifying
    @Query("update Notification n set n.readAt = CURRENT_TIMESTAMP "
            + "where n.recipient = :user and n.readAt is null")
    int markAllRead(@Param("user") User user);
}
