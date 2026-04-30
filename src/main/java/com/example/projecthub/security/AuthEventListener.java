package com.example.projecthub.security;

import com.example.projecthub.entity.User;
import com.example.projecthub.repository.UserRepository;
import com.example.projecthub.service.AuditService;
import java.time.LocalDateTime;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Слушатель Spring Security: считает неудачные попытки входа и блокирует аккаунт
 * после {@value #MAX_FAILED_ATTEMPTS} провалов на {@value #LOCK_DURATION_MINUTES} минут.
 * Параллельно пишет в audit log.
 */
@Component
public class AuthEventListener {

    public static final int MAX_FAILED_ATTEMPTS = 5;
    public static final int LOCK_DURATION_MINUTES = 15;

    private static final Logger log = LoggerFactory.getLogger(AuthEventListener.class);

    private final UserRepository userRepository;
    private final AuditService auditService;

    public AuthEventListener(UserRepository userRepository, AuditService auditService) {
        this.userRepository = userRepository;
        this.auditService = auditService;
    }

    @EventListener
    @Transactional
    public void onSuccess(AuthenticationSuccessEvent event) {
        String login = event.getAuthentication().getName();
        Optional<User> opt = userRepository.findByLogin(login);
        opt.ifPresent(user -> {
            if (user.getFailedLoginAttempts() != 0 || user.getLockedUntil() != null) {
                user.setFailedLoginAttempts(0);
                user.setLockedUntil(null);
                userRepository.save(user);
            }
        });
        auditService.record("LOGIN_SUCCESS", login, null);
    }

    @EventListener
    @Transactional
    public void onFailure(AbstractAuthenticationFailureEvent event) {
        String login = event.getAuthentication() != null ? event.getAuthentication().getName() : "?";
        String reason = event.getException() != null ? event.getException().getClass().getSimpleName() : "unknown";
        Optional<User> opt = userRepository.findByLogin(login);
        opt.ifPresent(user -> {
            int attempts = user.getFailedLoginAttempts() + 1;
            user.setFailedLoginAttempts(attempts);
            if (attempts >= MAX_FAILED_ATTEMPTS) {
                user.setLockedUntil(LocalDateTime.now().plusMinutes(LOCK_DURATION_MINUTES));
                log.warn("Аккаунт login={} заблокирован на {} минут после {} неудачных попыток",
                        login, LOCK_DURATION_MINUTES, attempts);
                auditService.record("ACCOUNT_LOCKED", login,
                        "после " + attempts + " неудачных попыток на " + LOCK_DURATION_MINUTES + " мин.");
            }
            userRepository.save(user);
        });
        auditService.record("LOGIN_FAILURE", login, "reason=" + reason);
    }
}
