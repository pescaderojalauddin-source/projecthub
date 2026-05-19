package com.example.projecthub.controller;

import com.example.projecthub.entity.User;
import com.example.projecthub.service.CurrentUserService;
import com.example.projecthub.service.EmailNotificationService;
import com.example.projecthub.service.UserService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

// личные настройки тек юзера: email и подписка на email-уведомления также содержит
@Controller
public class SettingsController {

    private final CurrentUserService currentUserService;
    private final UserService userService;
    private final EmailNotificationService emailNotificationService;

    public SettingsController(CurrentUserService currentUserService,
                              UserService userService,
                              EmailNotificationService emailNotificationService) {
        this.currentUserService = currentUserService;
        this.userService = userService;
        this.emailNotificationService = emailNotificationService;
    }

    @GetMapping("/settings")
    public String view(Model model) {
        User current = currentUserService.getCurrent();
        model.addAttribute("user", current);
        return "settings/view";
    }

    @PostMapping("/settings/notifications")
    public String updateNotifications(@RequestParam(value = "email", required = false) String email,
                                      @RequestParam(value = "emailNotifications", required = false) Boolean emailNotifications,
                                      RedirectAttributes redirectAttributes) {
        User current = currentUserService.getCurrent();
        userService.updateNotificationSettings(current, email, emailNotifications != null && emailNotifications);
        redirectAttributes.addFlashAttribute("flashSuccess", "Настройки сохранены.");
        return "redirect:/settings";
    }

    @PostMapping("/admin/notifications/run")
    @PreAuthorize("hasRole('ADMIN')")
    public String runDigestNow(RedirectAttributes redirectAttributes) {
        int sent = emailNotificationService.runOnce();
        redirectAttributes.addFlashAttribute("flashSuccess",
                "Email-дайджест запущен. Отправлено/смоделировано писем: " + sent + ".");
        return "redirect:/settings";
    }
}
