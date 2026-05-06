package com.example.projecthub.service;

import com.example.projecthub.entity.AuditLog;
import com.example.projecthub.repository.AuditLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Сервис журнала аудита. Лог-таблица read-only с точки зрения UI; писать только через этот сервис.
 *
 * <p>Известные действия (значения колонки {@code action}):
 * <ul>
 *   <li>{@code LOGIN_SUCCESS} / {@code LOGIN_FAILURE} / {@code ACCOUNT_LOCKED} — события аутентификации</li>
 *   <li>{@code PASSWORD_CHANGED} — пользователь сменил свой пароль</li>
 *   <li>{@code ROLE_CHANGED} — админ сменил роль пользователю (target = userId)</li>
 *   <li>{@code PROJECT_CREATED} / {@code PROJECT_UPDATED} / {@code PROJECT_DELETED}</li>
 *   <li>{@code TASK_CREATED} / {@code TASK_UPDATED} / {@code TASK_STATUS_CHANGED} / {@code TASK_DELETED}</li>
 * </ul>
 */
@Service
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);

    private final AuditLogRepository repository;

    public AuditService(AuditLogRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void record(String action, String targetType, Long targetId, String details) {
        AuditLog entry = new AuditLog(action, currentActor(), targetType, targetId, truncate(details), currentIp());
        repository.save(entry);
        log.debug("audit: action={} actor={} target={}#{} details={}",
                action, entry.getActor(), targetType, targetId, entry.getDetails());
    }

    @Transactional
    public void record(String action, String actor, String details) {
        AuditLog entry = new AuditLog(action, actor, null, null, truncate(details), currentIp());
        repository.save(entry);
    }

    @Transactional(readOnly = true)
    public Page<AuditLog> page(Pageable pageable) {
        return repository.findAllByOrderByCreatedAtDesc(pageable);
    }

    @Transactional(readOnly = true)
    public Page<AuditLog> page(String action, Pageable pageable) {
        if (action == null || action.isBlank()) {
            return page(pageable);
        }
        return repository.findAllByActionOrderByCreatedAtDesc(action, pageable);
    }

    private String currentActor() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return null;
        }
        return auth.getName();
    }

    private String currentIp() {
        try {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) {
                return null;
            }
            HttpServletRequest req = attrs.getRequest();
            String forwarded = req.getHeader("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank()) {
                return forwarded.split(",")[0].trim();
            }
            return req.getRemoteAddr();
        } catch (Exception ex) {
            return null;
        }
    }

    private String truncate(String s) {
        if (s == null) return null;
        return s.length() > 1000 ? s.substring(0, 997) + "..." : s;
    }
}
