package com.example.projecthub.repository;

import com.example.projecthub.entity.Comment;
import com.example.projecthub.entity.Task;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

// репозиторий комментариев к задачам
public interface CommentRepository extends JpaRepository<Comment, Long> {

    // все комментарии задачи в хронологическом порядке (старые → новые), с предзагруженным
    @EntityGraph(attributePaths = "author")
    List<Comment> findAllByTaskOrderByCreatedAtAsc(Task task);

    // подсчёт комментариев у задачи
    long countByTask(Task task);
}
