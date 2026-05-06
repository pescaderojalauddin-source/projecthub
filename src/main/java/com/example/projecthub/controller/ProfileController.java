package com.example.projecthub.controller;

import com.example.projecthub.dto.PasswordChangeForm;
import com.example.projecthub.entity.User;
import com.example.projecthub.service.CurrentUserService;
import com.example.projecthub.service.UserService;
import jakarta.validation.Valid;
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

    public ProfileController(CurrentUserService currentUserService, UserService userService) {
        this.currentUserService = currentUserService;
        this.userService = userService;
    }

    @GetMapping
    public String profile(Model model) {
        User user = currentUserService.getCurrent();
        model.addAttribute("user", user);
        if (!model.containsAttribute("passwordForm")) {
            model.addAttribute("passwordForm", new PasswordChangeForm());
        }
        return "profile/view";
    }

    @PostMapping("/password")
    public String changePassword(@Valid @ModelAttribute("passwordForm") PasswordChangeForm form,
                                 BindingResult bindingResult,
                                 Model model,
                                 RedirectAttributes redirectAttributes) {
        User user = currentUserService.getCurrent();
        if (bindingResult.hasErrors()) {
            model.addAttribute("user", user);
            return "profile/view";
        }
        try {
            userService.changePassword(user, form.getCurrentPassword(),
                    form.getNewPassword(), form.getNewPasswordConfirm());
        } catch (IllegalArgumentException ex) {
            bindingResult.reject("password", ex.getMessage());
            model.addAttribute("user", user);
            return "profile/view";
        }
        redirectAttributes.addFlashAttribute("flashSuccess", "Пароль успешно изменён.");
        return "redirect:/profile";
    }
}
