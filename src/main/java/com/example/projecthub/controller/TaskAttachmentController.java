package com.example.projecthub.controller;

import com.example.projecthub.entity.Task;
import com.example.projecthub.entity.TaskAttachment;
import com.example.projecthub.entity.User;
import com.example.projecthub.service.CurrentUserService;
import com.example.projecthub.service.TaskAttachmentService;
import com.example.projecthub.service.TaskService;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Загрузка/скачивание/удаление вложений к задачам.
 * Все RBAC-проверки выполняются в {@link TaskAttachmentService}.
 */
@Controller
public class TaskAttachmentController {

    private final TaskAttachmentService attachmentService;
    private final TaskService taskService;
    private final CurrentUserService currentUserService;

    public TaskAttachmentController(TaskAttachmentService attachmentService,
                                    TaskService taskService,
                                    CurrentUserService currentUserService) {
        this.attachmentService = attachmentService;
        this.taskService = taskService;
        this.currentUserService = currentUserService;
    }

    @PostMapping("/tasks/{taskId}/attachments")
    public String upload(@PathVariable Long taskId,
                         @RequestParam("file") MultipartFile file,
                         RedirectAttributes redirectAttributes) throws IOException {
        User current = currentUserService.getCurrent();
        Task task = taskService.getByIdForUser(taskId, current);
        try {
            attachmentService.upload(task, file, current);
            redirectAttributes.addFlashAttribute("flashSuccess", "Файл прикреплён.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("flashError", ex.getMessage());
        }
        return "redirect:/tasks/" + taskId;
    }

    @GetMapping("/tasks/{taskId}/attachments/{attId}/download")
    public ResponseEntity<FileSystemResource> download(@PathVariable Long taskId,
                                                       @PathVariable Long attId) throws IOException {
        User current = currentUserService.getCurrent();
        TaskAttachment att = attachmentService.getForUser(attId, current);
        if (!att.getTask().getId().equals(taskId)) {
            return ResponseEntity.notFound().build();
        }
        Path path = Path.of(att.getStoragePath());
        if (!Files.exists(path)) {
            return ResponseEntity.notFound().build();
        }
        String encoded = URLEncoder.encode(att.getFilename(), StandardCharsets.UTF_8).replace("+", "%20");
        FileSystemResource resource = new FileSystemResource(path);
        return ResponseEntity.ok()
                .contentType(att.getContentType() != null
                        ? MediaType.parseMediaType(att.getContentType())
                        : MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"download\"; filename*=UTF-8''" + encoded)
                .contentLength(att.getSizeBytes() != null ? att.getSizeBytes() : Files.size(path))
                .body(resource);
    }

    @PostMapping("/tasks/{taskId}/attachments/{attId}/delete")
    public String delete(@PathVariable Long taskId,
                         @PathVariable Long attId,
                         RedirectAttributes redirectAttributes) {
        User current = currentUserService.getCurrent();
        attachmentService.delete(attId, current);
        redirectAttributes.addFlashAttribute("flashSuccess", "Вложение удалено.");
        return "redirect:/tasks/" + taskId;
    }
}
