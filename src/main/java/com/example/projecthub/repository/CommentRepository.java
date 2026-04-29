package com.example.projecthub.repository;

import com.example.projecthub.entity.Comment;
import com.example.projecthub.entity.Task;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    List<Comment> findAllByTaskOrderByCreatedAtAsc(Task task);

    long countByTask(Task task);
}
