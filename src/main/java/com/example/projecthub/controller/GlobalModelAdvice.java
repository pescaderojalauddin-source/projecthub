package com.example.projecthub.controller;

import com.example.projecthub.service.CurrentUserService;
import com.example.projecthub.service.NotificationService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Заполняет общие атрибуты для всех HTML-страниц (навбар).
 *
 * <p>Сейчас это счётчик непрочитанных уведомлений {@code navUnreadCount},
 * чтобы layout мог показать badge на колокольчике без дублирования кода в каждом контроллере.</p>
 */
@ControllerAdvice(annotations = org.springframework.stereotype.Controller.class)
public class GlobalModelAdvice {

    private final CurrentUserService currentUserService;
    private final NotificationService notificationService;

    public GlobalModelAdvice(CurrentUserService currentUserService,
                             NotificationService notificationService) {
        this.currentUserService = currentUserService;
        this.notificationService = notificationService;
    }

    @ModelAttribute
    public void populate(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            model.addAttribute("navUnreadCount", 0L);
            return;
        }
        try {
            long unread = notificationService.unreadCount(currentUserService.getCurrent());
            model.addAttribute("navUnreadCount", unread);
        } catch (RuntimeException ex) {
            model.addAttribute("navUnreadCount", 0L);
        }
    }
}
