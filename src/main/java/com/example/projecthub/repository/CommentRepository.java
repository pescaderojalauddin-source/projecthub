package com.example.projecthub.repository;

import com.example.projecthub.entity.Comment;
import com.example.projecthub.entity.Task;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Репозиторий комментариев к задачам.
 */
public interface CommentRepository extends JpaRepository<Comment, Long> {

    /**
     * Все комментарии задачи в хронологическом порядке (старые → новые), с предзагруженным
     * автором: шаблон обращается к {@code c.author.login} после закрытия транзакции.
     */
    @EntityGraph(attributePaths = "author")
    List<Comment> findAllByTaskOrderByCreatedAtAsc(Task task);

    /** Подсчёт комментариев у задачи. */
    long countByTask(Task task);
}
