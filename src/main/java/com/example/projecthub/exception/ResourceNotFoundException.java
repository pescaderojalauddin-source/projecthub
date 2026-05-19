package com.example.projecthub.exception;

// бросается, когда сущность не найдена в БД
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
