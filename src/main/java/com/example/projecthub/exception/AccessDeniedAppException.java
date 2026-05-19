package com.example.projecthub.exception;

// бросается когда у юзера нет прав на действие
// отд класс чтоб не путаться со Spring's AccessDeniedException
public class AccessDeniedAppException extends RuntimeException {
    public AccessDeniedAppException(String message) {
        super(message);
    }
}
