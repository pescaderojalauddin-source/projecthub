package com.example.projecthub.entity;

/**
 * Приоритет задачи. Используется для сортировки в канбане/списке.
 * Порядок объявления = порядок убывания важности (URGENT — самый важный, LOW — самый низкий).
 */
public enum TaskPriority {
    URGENT("Срочно",  "bi-fire",          "bg-danger",          1),
    HIGH  ("Высокий", "bi-arrow-up-circle-fill", "bg-warning text-dark", 2),
    MEDIUM("Средний", "bi-dash-circle",   "bg-secondary",       3),
    LOW   ("Низкий",  "bi-arrow-down-circle", "bg-info text-dark",4);

    private final String label;
    private final String icon;
    private final String badgeClass;
    /** Числовой ранг: чем меньше — тем важнее. */
    private final int rank;

    TaskPriority(String label, String icon, String badgeClass, int rank) {
        this.label = label;
        this.icon = icon;
        this.badgeClass = badgeClass;
        this.rank = rank;
    }

    public String getLabel() {
        return label;
    }

    public String getIcon() {
        return icon;
    }

    public String getBadgeClass() {
        return badgeClass;
    }

    public int getRank() {
        return rank;
    }
}
