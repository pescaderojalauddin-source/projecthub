package com.example.projecthub.service;

import com.example.projecthub.entity.NotificationType;
import com.example.projecthub.entity.Project;
import com.example.projecthub.entity.User;
import com.example.projecthub.event.CommentAddedEvent;
import com.example.projecthub.event.TaskAssignedEvent;
import com.example.projecthub.event.TaskStatusChangedEvent;
import com.example.projecthub.repository.ProjectRepository;
import com.example.projecthub.repository.UserRepository;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Асинхронный обработчик доменных событий, который порождает уведомления.
 *
 * <p>Слушатели срабатывают <b>после успешного коммита основной транзакции</b>
 * ({@link TransactionPhase#AFTER_COMMIT}), чтобы не плодить уведомления при rollback,
 * и работают на отдельном пуле через {@code @Async} — поэтому медленные операции
 * (логирование, нотификации, в будущем e-mail) не задерживают HTTP-ответ.</p>
 *
 * <p>События переносят только идентификаторы и snapshot-строки; сущности перечитываются
 * заново внутри собственной транзакции, чтобы не зависеть от закрытой Hibernate-сессии.</p>
 */
@Component
public class NotificationListener {

    private static final Logger log = LoggerFactory.getLogger(NotificationListener.class);

    private final NotificationService notificationService;
    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;

    public NotificationListener(NotificationService notificationService,
                                UserRepository userRepository,
                                ProjectRepository projectRepository) {
        this.notificationService = notificationService;
        this.userRepository = userRepository;
        this.projectRepository = projectRepository;
    }

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTaskAssigned(TaskAssignedEvent event) {
        if (event.assigneeId() == null) {
            return;
        }
        Optional<User> assignee = userRepository.findById(event.assigneeId());
        if (assignee.isEmpty()) {
            return;
        }
        if (event.actorLogin() != null && event.actorLogin().equals(assignee.get().getLogin())) {
            return;
        }
        notificationService.create(
                assignee.get(),
                NotificationType.TASK_ASSIGNED,
                "На вас назначена задача",
                event.taskTitle(),
                "/tasks/" + event.taskId()
        );
        log.info("notify TASK_ASSIGNED → {} (taskId={}, by={})",
                assignee.get().getLogin(), event.taskId(), event.actorLogin());
    }

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTaskStatusChanged(TaskStatusChangedEvent event) {
        Optional<User> assignee = event.assigneeId() == null
                ? Optional.empty()
                : userRepository.findById(event.assigneeId());
        Optional<User> owner = ownerOf(event.projectId());

        String message = "Статус: " + (event.oldStatus() != null ? event.oldStatus() : "—")
                + " → " + event.newStatus();
        String title = "Статус задачи изменён";
        String body = event.taskTitle() + " · " + message;
        String link = "/tasks/" + event.taskId();

        assignee.filter(a -> notMatch(event.actorLogin(), a))
                .ifPresent(a -> notificationService.create(a, NotificationType.TASK_STATUS_CHANGED,
                        title, body, link));

        owner.filter(o -> notMatch(event.actorLogin(), o)
                        && (assignee.isEmpty() || !o.getId().equals(assignee.get().getId())))
                .ifPresent(o -> notificationService.create(o, NotificationType.TASK_STATUS_CHANGED,
                        title, body, link));

        log.info("notify TASK_STATUS_CHANGED taskId={} {} → {} by={}",
                event.taskId(), event.oldStatus(), event.newStatus(), event.actorLogin());
    }

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCommentAdded(CommentAddedEvent event) {
        Optional<User> assignee = event.assigneeId() == null
                ? Optional.empty()
                : userRepository.findById(event.assigneeId());
        Optional<User> owner = ownerOf(event.projectId());

        String title = truncate(event.authorLogin() + " прокомментировал «" + event.taskTitle() + "»", 200);
        String preview = event.textPreview();
        String link = "/tasks/" + event.taskId();

        assignee.filter(a -> !a.getId().equals(event.authorId()))
                .ifPresent(a -> notificationService.create(a, NotificationType.COMMENT_ADDED,
                        title, preview, link));

        owner.filter(o -> !o.getId().equals(event.authorId())
                        && (assignee.isEmpty() || !o.getId().equals(assignee.get().getId())))
                .ifPresent(o -> notificationService.create(o, NotificationType.COMMENT_ADDED,
                        title, preview, link));

        log.info("notify COMMENT_ADDED taskId={} by={}", event.taskId(), event.authorLogin());
    }

    private Optional<User> ownerOf(Long projectId) {
        if (projectId == null) {
            return Optional.empty();
        }
        return projectRepository.findById(projectId).map(Project::getOwner);
    }

    private static boolean notMatch(String actorLogin, User user) {
        return actorLogin == null || !actorLogin.equals(user.getLogin());
    }

    /** Усечь строку по максимальной длине колонки в БД, добавив многоточие. */
    private static String truncate(String s, int max) {
        if (s == null || s.length() <= max) {
            return s;
        }
        return s.substring(0, max - 1) + "…";
    }
}
