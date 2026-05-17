package com.example.projecthub.repository;

import com.example.projecthub.entity.Task;
import com.example.projecthub.entity.TaskAttachment;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Репозиторий метаданных файлов, прикреплённых к задачам. */
public interface TaskAttachmentRepository extends JpaRepository<TaskAttachment, Long> {
    /** Список вложений конкретной задачи, упорядоченный по дате загрузки (новые сверху). */
    List<TaskAttachment> findByTaskOrderByUploadedAtDesc(Task task);
}
