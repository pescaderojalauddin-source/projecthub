package com.example.projecthub.event;

/**
 * Доменное событие: к задаче добавлен комментарий.
 *
 * @param taskId       id задачи
 * @param taskTitle    заголовок задачи (snapshot)
 * @param projectId    id проекта (для определения владельца-получателя)
 * @param assigneeId   id исполнителя (если есть)
 * @param authorId     id автора комментария
 * @param authorLogin  логин автора (для текста уведомления)
 * @param textPreview  превью текста (до 200 символов)
 */
public record CommentAddedEvent(Long taskId, String taskTitle, Long projectId,
                                Long assigneeId, Long authorId, String authorLogin,
                                String textPreview) {
}
