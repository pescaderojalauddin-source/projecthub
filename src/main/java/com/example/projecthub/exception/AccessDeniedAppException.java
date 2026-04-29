package com.example.projecthub.exception;

/**
 * Бросается, когда у пользователя нет прав на действие.
 * Отдельный класс используется, чтобы не путать с {@code org.springframework.security.access.AccessDeniedException}
 * на уровне контроллеров.
 */
public class AccessDeniedAppException extends RuntimeException {
    public AccessDeniedAppException(String message) {
        super(message);
    }
}
