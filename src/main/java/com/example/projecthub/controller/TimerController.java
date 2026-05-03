package com.example.projecthub.controller;

import com.example.projecthub.entity.TimeEntry;
import com.example.projecthub.entity.User;
import com.example.projecthub.service.CurrentUserService;
import com.example.projecthub.service.TimerService;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Контроллер таймеров: запуск/остановка по задаче (как форма и как AJAX),
 * а также мини-эндпоинт для индикатора в навбаре.
 */
@Controller
public class TimerController {

    private final TimerService timerService;
    private final CurrentUserService currentUserService;

    public TimerController(TimerService timerService, CurrentUserService currentUserService) {
        this.timerService = timerService;
        this.currentUserService = currentUserService;
    }

    @PostMapping("/tasks/{id}/timer/start")
    public String start(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        User user = currentUserService.getCurrent();
        timerService.start(id, user);
        redirectAttributes.addFlashAttribute("flashSuccess", "Таймер запущен.");
        return "redirect:/tasks/" + id;
    }

    @PostMapping("/tasks/{id}/timer/stop")
    public String stopFromTask(@PathVariable Long id,
                               @RequestParam(value = "note", required = false) String note,
                               RedirectAttributes redirectAttributes) {
        User user = currentUserService.getCurrent();
        Optional<TimeEntry> stopped = timerService.stopActive(user, note);
        if (stopped.isPresent()) {
            long sec = stopped.get().getDurationSeconds() != null ? stopped.get().getDurationSeconds() : 0L;
            redirectAttributes.addFlashAttribute("flashSuccess",
                    "Таймер остановлен · зафиксировано " + TimerService.formatDuration(sec) + ".");
        } else {
            redirectAttributes.addFlashAttribute("flashError", "Активного таймера не было.");
        }
        return "redirect:/tasks/" + id;
    }

    /** Глобальный «Стоп» (например, из навбара) — возвращает на referer/main. */
    @PostMapping("/timer/stop")
    public String stopGlobal(@RequestParam(value = "redirect", required = false) String redirect,
                             RedirectAttributes redirectAttributes) {
        User user = currentUserService.getCurrent();
        Optional<TimeEntry> stopped = timerService.stopActive(user, null);
        if (stopped.isPresent()) {
            long sec = stopped.get().getDurationSeconds() != null ? stopped.get().getDurationSeconds() : 0L;
            redirectAttributes.addFlashAttribute("flashSuccess",
                    "Таймер остановлен · зафиксировано " + TimerService.formatDuration(sec) + ".");
        }
        // Защита от open redirect: принимаем только локальные пути ("/..."),
        // отклоняем protocol-relative URL ("//evil.com") и прочие схемы.
        String safe = (redirect == null
                || redirect.isBlank()
                || !redirect.startsWith("/")
                || redirect.startsWith("//")
                || redirect.startsWith("/\\")) ? "/" : redirect;
        return "redirect:" + safe;
    }

    /** AJAX-эндпоинт для индикатора навбара: возвращает { active, taskId, taskTitle, startedAt, seconds }. */
    @GetMapping("/timer/active.json")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> active() {
        User user = currentUserService.getCurrent();
        Map<String, Object> body = new LinkedHashMap<>();
        Optional<TimeEntry> active = timerService.getActiveEntry(user);
        if (active.isEmpty()) {
            body.put("active", false);
            return ResponseEntity.ok(body);
        }
        TimeEntry entry = active.get();
        body.put("active", true);
        body.put("taskId", entry.getTask().getId());
        body.put("taskTitle", entry.getTask().getTitle());
        body.put("startedAt", entry.getStartAt().toString());
        long elapsed = java.time.Duration.between(entry.getStartAt(), timerService.now()).getSeconds();
        body.put("seconds", Math.max(0L, elapsed));
        return ResponseEntity.ok(body);
    }
}
