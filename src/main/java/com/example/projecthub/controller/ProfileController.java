package com.example.projecthub.controller;

import com.example.projecthub.dto.PasswordChangeForm;
import com.example.projecthub.entity.User;
import com.example.projecthub.service.CurrentUserService;
import com.example.projecthub.service.TimerService;
import com.example.projecthub.service.UserService;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Профиль текущего пользователя: просмотр данных и смена пароля.
 */
@Controller
@RequestMapping("/profile")
@PreAuthorize("isAuthenticated()")
public class ProfileController {

    private final CurrentUserService currentUserService;
    private final UserService userService;
    private final TimerService timerService;

    public ProfileController(CurrentUserService currentUserService,
                             UserService userService,
                             TimerService timerService) {
        this.currentUserService = currentUserService;
        this.userService = userService;
        this.timerService = timerService;
    }

    /** Общие атрибуты для всех вью профиля: время и история таймеров. */
    @ModelAttribute
    public void populateCommon(Model model) {
        User user = currentUserService.getCurrent();
        model.addAttribute("user", user);
        model.addAttribute("timeEntries",
                timerService.historyForUser(user, PageRequest.of(0, 20)));
        model.addAttribute("totalSecondsToday", timerService.totalSecondsForUserToday(user));
        model.addAttribute("totalSecondsLast7Days", timerService.totalSecondsForUserLast7Days(user));
        model.addAttribute("timerService", timerService);
    }

    @GetMapping
    public String profile(Model model) {
        if (!model.containsAttribute("passwordForm")) {
            model.addAttribute("passwordForm", new PasswordChangeForm());
        }
        return "profile/view";
    }

    @PostMapping("/password")
    public String changePassword(@Valid @ModelAttribute("passwordForm") PasswordChangeForm form,
                                 BindingResult bindingResult,
                                 RedirectAttributes redirectAttributes) {
        User user = currentUserService.getCurrent();
        if (bindingResult.hasErrors()) {
            return "profile/view";
        }
        try {
            userService.changePassword(user, form.getCurrentPassword(),
                    form.getNewPassword(), form.getNewPasswordConfirm());
        } catch (IllegalArgumentException ex) {
            bindingResult.reject("password", ex.getMessage());
            return "profile/view";
        }
        redirectAttributes.addFlashAttribute("flashSuccess", "Пароль успешно изменён.");
        return "redirect:/profile";
    }
}
