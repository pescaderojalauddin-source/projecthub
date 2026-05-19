package com.example.projecthub.controller;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

// корневой контр — редирект с / на логин или список проектов
@Controller
public class HomeController {

    // редиректит неавторизованного на форму логина, авторизованного — на /dashboard
    @GetMapping("/")
    public String home(Authentication auth) {
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            return "redirect:/dashboard";
        }
        return "redirect:/login";
    }
}
