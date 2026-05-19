package com.example.projecthub.entity;

// статус проекта
public enum ProjectStatus {
    ACTIVE("Активный"),
    ARCHIVED("В архиве"),
    COMPLETED("Завершён");

    private final String label;

    ProjectStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
