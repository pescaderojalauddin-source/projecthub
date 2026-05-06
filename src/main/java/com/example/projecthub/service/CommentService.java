package com.example.projecthub.service;

import com.example.projecthub.dto.CommentForm;
import com.example.projecthub.entity.Comment;
import com.example.projecthub.entity.Role;
import com.example.projecthub.entity.Task;
import com.example.projecthub.entity.User;
import com.example.projecthub.event.CommentAddedEvent;
import com.example.projecthub.exception.AccessDeniedAppException;
import com.example.projecthub.exception.ResourceNotFoundException;
import com.example.projecthub.repository.CommentRepository;
import java.util.List;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Сервис комментариев. */
@Service
@Transactional
public class CommentService {

    private final CommentRepository commentRepository;
    private final TaskService taskService;
    private final ApplicationEventPublisher events;

    public CommentService(CommentRepository commentRepository, TaskService taskService,
                          ApplicationEventPublisher events) {
        this.commentRepository = commentRepository;
        this.taskService = taskService;
        this.events = events;
    }

    @Transactional(readOnly = true)
    public List<Comment> listByTask(Task task) {
        return commentRepository.findAllByTaskOrderByCreatedAtAsc(task);
    }

    public Comment add(Task task, CommentForm form, User author) {
        taskService.ensureAccessible(task, author);
        Comment comment = new Comment(form.getText(), task, author);
        Comment saved = commentRepository.save(comment);
        String preview = saved.getText();
        if (preview != null && preview.length() > 200) {
            preview = preview.substring(0, 197) + "…";
        }
        events.publishEvent(new CommentAddedEvent(
                task.getId(),
                task.getTitle(),
                task.getProject() != null ? task.getProject().getId() : null,
                task.getAssignee() != null ? task.getAssignee().getId() : null,
                author.getId(),
                author.getLogin(),
                preview));
        return saved;
    }

    public void delete(Long commentId, User actor) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Комментарий не найден: id=" + commentId));
        if (actor.getRole() != Role.ADMIN && !comment.getAuthor().getId().equals(actor.getId())) {
            throw new AccessDeniedAppException("Нет прав на удаление комментария");
        }
        commentRepository.delete(comment);
    }
}
