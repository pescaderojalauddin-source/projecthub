package com.example.projecthub.event;

/**
 * Доменное событие: задача назначена на пользователя (новая или сменился исполнитель).
 *
 * <p>Содержит только идентификаторы и строки, чтобы безопасно обрабатываться в
 * {@code @Async @TransactionalEventListener(AFTER_COMMIT)} на другом потоке вне исходной
 * Hibernate-сессии. Слушатель сам перечитает сущности по id.</p>
 *
 * @param taskId        id задачи
 * @param taskTitle     заголовок задачи (snapshot)
 * @param assigneeId    id назначенного исполнителя
 * @param actorLogin    логин пользователя, инициировавшего изменение
 */
public record TaskAssignedEvent(Long taskId, String taskTitle, Long assigneeId, String actorLogin) {
}
