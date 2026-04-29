package com.example.projecthub.controller;

import com.example.projecthub.entity.Role;
import com.example.projecthub.entity.User;
import com.example.projecthub.service.StatisticsService;
import com.example.projecthub.service.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final UserService userService;
    private final StatisticsService statisticsService;

    public AdminController(UserService userService, StatisticsService statisticsService) {
        this.userService = userService;
        this.statisticsService = statisticsService;
    }

    @GetMapping("/users")
    public String users(@RequestParam(value = "search", required = false) String search,
                        @RequestParam(value = "page", defaultValue = "0") int page,
                        @RequestParam(value = "size", defaultValue = "20") int size,
                        Model model) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "login"));
        Page<User> users = userService.search(search, pageable);
        model.addAttribute("users", users);
        model.addAttribute("search", search);
        model.addAttribute("roles", Role.values());
        return "admin/users";
    }

    @PostMapping("/users/{id}/role")
    public String changeRole(@PathVariable Long id,
                             @RequestParam("role") Role role,
                             RedirectAttributes redirectAttributes) {
        User updated = userService.changeRole(id, role);
        redirectAttributes.addFlashAttribute("flashSuccess",
                "Пользователю " + updated.getLogin() + " назначена роль " + role.name() + ".");
        return "redirect:/admin/users";
    }

    @GetMapping("/stats")
    public String stats(Model model) {
        model.addAttribute("stats", statisticsService.snapshot());
        return "admin/stats";
    }
}
