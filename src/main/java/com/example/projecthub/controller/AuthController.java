package com.example.projecthub.controller;

import com.example.projecthub.dto.RegistrationForm;
import com.example.projecthub.service.UserService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Контроллер аутентификации: страница логина и регистрация. */
@Controller
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    /** Страница формы логина. */
    @GetMapping("/login")
    public String login() {
        return "auth/login";
    }

    /** Страница формы регистрации. */
    @GetMapping("/register")
    public String registerForm(Model model) {
        if (!model.containsAttribute("form")) {
            model.addAttribute("form", new RegistrationForm());
        }
        return "auth/register";
    }

    /** Обработка регистрации: валидация формы, создание USER, редирект на логин. */
    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("form") RegistrationForm form,
                           BindingResult bindingResult,
                           RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "auth/register";
        }
        try {
            userService.register(form);
        } catch (IllegalArgumentException ex) {
            bindingResult.reject("registration", ex.getMessage());
            return "auth/register";
        }
        redirectAttributes.addFlashAttribute("flashSuccess", "Регистрация успешна, войдите в систему.");
        return "redirect:/login";
    }
}
