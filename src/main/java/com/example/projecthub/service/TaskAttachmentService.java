package com.example.projecthub.service;

import com.example.projecthub.entity.Role;
import com.example.projecthub.entity.Task;
import com.example.projecthub.entity.TaskAttachment;
import com.example.projecthub.entity.User;
import com.example.projecthub.exception.AccessDeniedAppException;
import com.example.projecthub.exception.ResourceNotFoundException;
import com.example.projecthub.repository.TaskAttachmentRepository;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * Сервис вложений к задачам. Файлы хранятся на диске в
 * {@code projecthub.uploads.dir} (по умолчанию {@code uploads/}),
 * метаданные — в БД через {@link TaskAttachmentRepository}.
 *
 * <p>Ограничения:
 * <ul>
 *   <li>максимальный размер — задаётся {@code spring.servlet.multipart.max-file-size};</li>
 *   <li>исходное имя файла нормализуется (не позволяем path-traversal);</li>
 *   <li>фактический файл сохраняется с UUID-префиксом — это исключает коллизии и
 *       угадывание пути для скачивания.</li>
 * </ul>
 *
 * <p>RBAC: загружать/удалять вложения могут владелец проекта, исполнитель задачи или ADMIN.
 */
@Service
@Transactional
public class TaskAttachmentService {

    private final TaskAttachmentRepository attachmentRepository;
    private final TaskService taskService;
    private final Path uploadsDir;

    public TaskAttachmentService(TaskAttachmentRepository attachmentRepository,
                                 TaskService taskService,
                                 @Value("${projecthub.uploads.dir:uploads}") String uploadsDir) {
        this.attachmentRepository = attachmentRepository;
        this.taskService = taskService;
        this.uploadsDir = Path.of(uploadsDir).toAbsolutePath();
    }

    /** Список вложений задачи (с RBAC). */
    @Transactional(readOnly = true)
    public List<TaskAttachment> listForTask(Task task, User actor) {
        taskService.ensureAccessible(task, actor);
        return attachmentRepository.findByTaskOrderByUploadedAtDesc(task);
    }

    /** Возвращает вложение по id, проверяя доступ через родительскую задачу. */
    @Transactional(readOnly = true)
    public TaskAttachment getForUser(Long attachmentId, User actor) {
        TaskAttachment att = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Вложение не найдено: id=" + attachmentId));
        taskService.ensureAccessible(att.getTask(), actor);
        return att;
    }

    /** Загружает файл, сохраняет его на диск и создаёт запись метаданных. */
    public TaskAttachment upload(Task task, MultipartFile file, User actor) throws IOException {
        taskService.ensureAccessible(task, actor);
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Файл пустой или не выбран");
        }
        String original = sanitizeName(file.getOriginalFilename());
        String unique = UUID.randomUUID() + "_" + original;
        Path taskDir = uploadsDir.resolve(String.valueOf(task.getId()));
        Files.createDirectories(taskDir);
        Path target = taskDir.resolve(unique);
        try (var in = file.getInputStream()) {
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        }
        TaskAttachment att = new TaskAttachment(
                task,
                original,
                target.toAbsolutePath().toString(),
                file.getContentType(),
                file.getSize(),
                actor.getLogin());
        return attachmentRepository.save(att);
    }

    /** Удаляет вложение с диска + запись из БД. */
    public void delete(Long attachmentId, User actor) {
        TaskAttachment att = getForUser(attachmentId, actor);
        // только владелец задачи, загрузивший или admin
        if (actor.getRole() != Role.ADMIN
                && !actor.getLogin().equals(att.getUploadedBy())
                && !att.getTask().getProject().getOwner().getId().equals(actor.getId())) {
            throw new AccessDeniedAppException("Удалять вложение может только автор, владелец проекта или ADMIN");
        }
        try {
            Files.deleteIfExists(Path.of(att.getStoragePath()));
        } catch (IOException ignored) {
            // файла может уже не быть — это не критично, главное удалить запись
        }
        attachmentRepository.delete(att);
    }

    /** Запрещает path-traversal (.., /, \) в имени файла. */
    private static String sanitizeName(String raw) {
        if (raw == null || raw.isBlank()) return "file";
        String trimmed = raw.replaceAll("[\\\\/]", "_").trim();
        if (trimmed.length() > 200) trimmed = trimmed.substring(trimmed.length() - 200);
        return trimmed;
    }
}
