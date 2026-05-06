package com.example.projecthub.event;

import com.example.projecthub.entity.TaskStatus;

/**
 * Доменное событие: статус задачи изменился.
 *
 * @param taskId      id задачи
 * @param taskTitle   заголовок задачи (snapshot)
 * @param projectId   id проекта (для определения владельца-получателя)
 * @param assigneeId  id исполнителя (если есть)
 * @param oldStatus   предыдущий статус
 * @param newStatus   новый статус
 * @param actorLogin  логин пользователя, сменившего статус
 */
public record TaskStatusChangedEvent(Long taskId, String taskTitle, Long projectId,
                                     Long assigneeId, TaskStatus oldStatus, TaskStatus newStatus,
                                     String actorLogin) {
}
