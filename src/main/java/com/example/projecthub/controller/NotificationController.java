package com.example.projecthub.controller;

import com.example.projecthub.entity.Notification;
import com.example.projecthub.entity.User;
import com.example.projecthub.service.CurrentUserService;
import com.example.projecthub.service.NotificationService;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * HTML/JSON эндпоинты для системных уведомлений.
 *
 * <ul>
 *     <li>GET /notifications — полный список с пагинацией</li>
 *     <li>GET /notifications/recent.json — последние 10 + счётчик непрочитанных (для навбара)</li>
 *     <li>POST /notifications/{id}/read — пометить одно прочитанным</li>
 *     <li>POST /notifications/read-all — пометить все прочитанными</li>
 * </ul>
 */
@Controller
@RequestMapping("/notifications")
@PreAuthorize("isAuthenticated()")
public class NotificationController {

    private static final int MAX_PAGE_SIZE = 100;

    private final NotificationService notificationService;
    private final CurrentUserService currentUserService;

    public NotificationController(NotificationService notificationService,
                                  CurrentUserService currentUserService) {
        this.notificationService = notificationService;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    public String list(@RequestParam(defaultValue = "0") int page,
                       @RequestParam(defaultValue = "20") int size,
                       Model model) {
        User user = currentUserService.getCurrent();
        int safePage = Math.max(0, page);
        int safeSize = Math.min(Math.max(1, size), MAX_PAGE_SIZE);
        Page<Notification> notifications = notificationService.page(user,
                PageRequest.of(safePage, safeSize));
        model.addAttribute("notifications", notifications);
        return "notifications/list";
    }

    @GetMapping("/recent.json")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> recent() {
        User user = currentUserService.getCurrent();
        List<Notification> recent = notificationService.recent(user);
        long unread = notificationService.unreadCount(user);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("unread", unread);
        body.put("items", recent.stream().map(this::toMap).toList());
        return ResponseEntity.ok(body);
    }

    @PostMapping("/{id}/read")
    public String markRead(@PathVariable Long id,
                           @RequestParam(value = "redirect", required = false) String redirect) {
        User user = currentUserService.getCurrent();
        notificationService.markRead(user, id);
        return "redirect:" + safeRedirect(redirect);
    }

    @PostMapping("/read-all")
    public String markAllRead(@RequestParam(value = "redirect", required = false) String redirect,
                              RedirectAttributes redirectAttributes) {
        User user = currentUserService.getCurrent();
        int affected = notificationService.markAllRead(user);
        redirectAttributes.addFlashAttribute("flashSuccess",
                affected > 0
                        ? "Помечено прочитанными: " + affected
                        : "Непрочитанных уведомлений нет.");
        return "redirect:" + safeRedirect(redirect);
    }

    private Map<String, Object> toMap(Notification n) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", n.getId());
        m.put("type", n.getType());
        m.put("title", n.getTitle());
        m.put("message", n.getMessage());
        m.put("link", n.getLink());
        m.put("createdAt", n.getCreatedAt());
        m.put("unread", n.isUnread());
        return m;
    }

    private static String safeRedirect(String redirect) {
        if (redirect == null
                || redirect.isBlank()
                || !redirect.startsWith("/")
                || redirect.startsWith("//")
                || redirect.startsWith("/\\")) {
            return "/notifications";
        }
        return redirect;
    }
}
