package com.example.projecthub.controller.api;

import com.example.projecthub.dto.CommentForm;
import com.example.projecthub.dto.api.CommentDto;
import com.example.projecthub.entity.Comment;
import com.example.projecthub.entity.Task;
import com.example.projecthub.entity.User;
import com.example.projecthub.service.CommentService;
import com.example.projecthub.service.CurrentUserService;
import com.example.projecthub.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

// rEST API для комментариев к задачам
@RestController
@RequestMapping("/api/v1")
@Tag(name = "Comments", description = "Комментарии к задачам")
public class CommentRestController {

    private final CommentService commentService;
    private final TaskService taskService;
    private final CurrentUserService currentUserService;

    public CommentRestController(CommentService commentService,
                                 TaskService taskService,
                                 CurrentUserService currentUserService) {
        this.commentService = commentService;
        this.taskService = taskService;
        this.currentUserService = currentUserService;
    }

    // список комментариев задачи (старые → новые)
    @GetMapping("/tasks/{taskId}/comments")
    @Operation(summary = "Список комментариев задачи")
    public List<CommentDto> list(@PathVariable Long taskId) {
        User current = currentUserService.getCurrent();
        Task task = taskService.getByIdForUser(taskId, current);
        return commentService.listByTask(task).stream().map(CommentDto::of).toList();
    }

    // добавить комментарий к задаче
    @PostMapping("/tasks/{taskId}/comments")
    @Operation(summary = "Добавить комментарий")
    public ResponseEntity<CommentDto> add(@PathVariable Long taskId,
                                          @Valid @RequestBody CommentForm form) {
        User current = currentUserService.getCurrent();
        Task task = taskService.getByIdForUser(taskId, current);
        Comment comment = commentService.add(task, form, current);
        return ResponseEntity
                .created(java.net.URI.create("/api/v1/comments/" + comment.getId()))
                .body(CommentDto.of(comment));
    }

    // удалить комментарий. Разрешено автору или ADMIN
    @DeleteMapping("/comments/{commentId}")
    @Operation(summary = "Удалить комментарий")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long commentId) {
        User current = currentUserService.getCurrent();
        commentService.delete(commentId, current);
    }
}
