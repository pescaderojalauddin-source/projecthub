package com.example.projecthub.entity;

// статус задачи внутри проекта
public enum TaskStatus {
    TODO("К выполнению"),
    IN_PROGRESS("В работе"),
    DONE("Готово"),
    BLOCKED("Заблокировано");

    private final String label;

    TaskStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
