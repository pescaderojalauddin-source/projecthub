package com.example.projecthub.service;

import com.example.projecthub.entity.User;
import com.example.projecthub.exception.AccessDeniedAppException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

// утилита для получения тек залогиненного юзера вынесена в отд сервис, чтобы не таскать
@Service
public class CurrentUserService {

    private final UserService userService;

    public CurrentUserService(UserService userService) {
        this.userService = userService;
    }

    // возвращает тек юзера или бросает AccessDeniedAppException
    public User getCurrent() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            throw new AccessDeniedAppException("Требуется авторизация");
        }
        return userService.findByLogin(auth.getName());
    }
}
