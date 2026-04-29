package com.example.projecthub.repository;

import com.example.projecthub.entity.Project;
import com.example.projecthub.entity.Task;
import com.example.projecthub.entity.TaskStatus;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskRepository extends JpaRepository<Task, Long> {

    Page<Task> findAllByProject(Project project, Pageable pageable);

    Page<Task> findAllByProjectAndStatus(Project project, TaskStatus status, Pageable pageable);

    List<Task> findAllByProject(Project project);

    long countByProject(Project project);

    long countByStatus(TaskStatus status);
}
