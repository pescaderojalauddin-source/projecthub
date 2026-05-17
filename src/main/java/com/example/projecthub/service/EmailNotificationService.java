package com.example.projecthub.service;

import com.example.projecthub.entity.Task;
import com.example.projecthub.entity.TaskStatus;
import com.example.projecthub.entity.User;
import com.example.projecthub.repository.TaskRepository;
import com.example.projecthub.repository.UserRepository;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Утренний email-дайджест по просроченным задачам.
 *
 * <p>Каждое утро по cron (по умолчанию 08:00) сервис собирает по каждому
 * пользователю с непустым email и {@code emailNotifications=true} список
 * просроченных задач (дедлайн раньше сегодняшнего дня, статус не DONE),
 * формирует короткое письмо и отправляет через {@link JavaMailSender}.
 *
 * <p>Включается флагом {@code projecthub.notifications.email.enabled=true}
 * (по умолчанию выключено — чтобы в локалке/CI не пытаться слать письма).
 * Если флаг включён, но {@code JavaMailSender} не сконфигурирован — сервис
 * молча пропускает рассылку и пишет в лог. Это позволяет демонстрировать
 * фичу без реального SMTP: вызови {@link #runOnce()} вручную, в логе будет
 * полный текст письма для каждого получателя.
 */
@Service
public class EmailNotificationService {

    private static final Logger log = LoggerFactory.getLogger(EmailNotificationService.class);
    private static final Collection<TaskStatus> NOT_DONE =
            List.of(TaskStatus.TODO, TaskStatus.IN_PROGRESS, TaskStatus.BLOCKED);

    private final UserRepository userRepository;
    private final TaskRepository taskRepository;
    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final boolean enabled;
    private final String fromAddress;

    public EmailNotificationService(UserRepository userRepository,
                                    TaskRepository taskRepository,
                                    ObjectProvider<JavaMailSender> mailSenderProvider,
                                    @Value("${projecthub.notifications.email.enabled:false}") boolean enabled,
                                    @Value("${projecthub.notifications.email.from:no-reply@projecthub.local}") String fromAddress) {
        this.userRepository = userRepository;
        this.taskRepository = taskRepository;
        this.mailSenderProvider = mailSenderProvider;
        this.enabled = enabled;
        this.fromAddress = fromAddress;
    }

    /** Crontab: каждый день в 08:00. Параметризовано {@code projecthub.notifications.email.cron}. */
    @Scheduled(cron = "${projecthub.notifications.email.cron:0 0 8 * * *}")
    public void sendDailyDigest() {
        if (!enabled) {
            log.debug("Email notifications disabled (projecthub.notifications.email.enabled=false), skipping.");
            return;
        }
        runOnce();
    }

    /** Сразу запустить рассылку (без расписания). Используется в админских хендлерах/тестах. */
    @Transactional(readOnly = true)
    public int runOnce() {
        LocalDate today = LocalDate.now();
        List<User> all = userRepository.findAll();
        int sent = 0;
        JavaMailSender sender = mailSenderProvider.getIfAvailable();

        for (User u : all) {
            if (!u.isEmailNotifications()) continue;
            if (u.getEmail() == null || u.getEmail().isBlank()) continue;

            List<Task> overdue = taskRepository
                    .findTop10ByAssigneeAndDeadlineBeforeAndStatusNotInOrderByDeadlineAsc(u, today, NOT_DONE);
            if (overdue.isEmpty()) continue;

            String body = buildBody(u, overdue);
            String subject = "ProjectHub: у вас " + overdue.size() + " просроченн" +
                    (overdue.size() == 1 ? "ая задача" : "ых задач");

            if (sender == null) {
                // Лог-only режим: показываем «как бы письмо» в логе.
                log.info("[email-dry-run] To: {} <{}>\nSubject: {}\n{}", u.getLogin(), u.getEmail(), subject, body);
                sent++;
                continue;
            }
            try {
                SimpleMailMessage msg = new SimpleMailMessage();
                msg.setFrom(fromAddress);
                msg.setTo(u.getEmail());
                msg.setSubject(subject);
                msg.setText(body);
                sender.send(msg);
                sent++;
            } catch (Exception ex) {
                log.warn("Не удалось отправить email пользователю {}: {}", u.getLogin(), ex.getMessage());
            }
        }
        log.info("Email digest: отправлено {} писем.", sent);
        return sent;
    }

    private String buildBody(User user, List<Task> overdue) {
        StringBuilder sb = new StringBuilder();
        sb.append("Здравствуйте, ").append(user.getLogin()).append("!\n\n");
        sb.append("У вас ").append(overdue.size()).append(" просроченн")
          .append(overdue.size() == 1 ? "ая задача" : "ых задач")
          .append(":\n\n");
        for (Task t : overdue) {
            sb.append("• #").append(t.getId()).append(" «").append(safeTitle(t.getTitle())).append("»");
            if (t.getDeadline() != null) {
                sb.append(" — дедлайн ").append(t.getDeadline());
            }
            sb.append(" (проект «").append(safeTitle(t.getProject().getTitle())).append("»)\n");
        }
        sb.append("\nПерейдите в ProjectHub, чтобы закрыть или перенести задачи.\n");
        sb.append("\n— ProjectHub");
        return sb.toString();
    }

    private static String safeTitle(String s) {
        return Objects.toString(s, "").replace('\n', ' ').replace('\r', ' ');
    }
}
