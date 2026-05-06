package com.example.projecthub.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.csrf.MissingCsrfTokenException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.NoHandlerFoundException;

/**
 * Глобальный обработчик исключений. Возвращает Thymeleaf-шаблоны страниц ошибок
 * и проставляет корректные HTTP-статусы.
 */
@ControllerAdvice(annotations = Controller.class)
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String notFound(ResourceNotFoundException ex, Model model) {
        model.addAttribute("message", ex.getMessage());
        return "error/404";
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String noHandler(NoHandlerFoundException ex, Model model) {
        model.addAttribute("message", "Страница не найдена: " + ex.getRequestURL());
        return "error/404";
    }

    @ExceptionHandler({AccessDeniedAppException.class, AccessDeniedException.class})
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public String forbidden(Exception ex, Model model) {
        model.addAttribute("message", ex.getMessage());
        return "error/403";
    }

    @ExceptionHandler(MissingCsrfTokenException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public String csrf(MissingCsrfTokenException ex, Model model) {
        model.addAttribute("message", "Сессия истекла или невалидный CSRF-токен. Перезайдите, пожалуйста.");
        return "error/403";
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String badRequest(IllegalArgumentException ex, Model model) {
        model.addAttribute("message", ex.getMessage());
        return "error/400";
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String serverError(Exception ex, HttpServletRequest request, Model model) {
        log.error("Необработанное исключение по запросу {}", request.getRequestURI(), ex);
        model.addAttribute("message", "Внутренняя ошибка сервера. Попробуйте позже.");
        return "error/500";
    }
}
