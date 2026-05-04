package com.example.projecthub.entity;

/**
 * Тип системного уведомления.
 */
public enum NotificationType {
    /** Задача назначена на пользователя (новая или сменился исполнитель). */
    TASK_ASSIGNED,
    /** Изменился статус задачи, в которой пользователь — исполнитель или владелец проекта. */
    TASK_STATUS_CHANGED,
    /** К задаче добавили комментарий. */
    COMMENT_ADDED,
    /** Скоро дедлайн (≤ 1 дня). */
    DEADLINE_SOON
}
