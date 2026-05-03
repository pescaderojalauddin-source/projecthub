package com.example.projecthub.controller;

import com.example.projecthub.service.CurrentUserService;
import com.example.projecthub.service.DashboardService;
import com.example.projecthub.service.TimerService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    private final DashboardService dashboardService;
    private final CurrentUserService currentUserService;
    private final TimerService timerService;

    public HomeController(DashboardService dashboardService,
                          CurrentUserService currentUserService,
                          TimerService timerService) {
        this.dashboardService = dashboardService;
        this.currentUserService = currentUserService;
        this.timerService = timerService;
    }

    @GetMapping("/")
    public String home(Authentication auth, Model model) {
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return "redirect:/login";
        }
        DashboardService.DashboardData data = dashboardService.snapshot(currentUserService.getCurrent());
        model.addAttribute("dashboard", data);
        model.addAttribute("timerService", timerService);
        return "home";
    }
}
