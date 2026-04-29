package com.example.projecthub.repository;

import com.example.projecthub.entity.Project;
import com.example.projecthub.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectRepository extends JpaRepository<Project, Long> {

    Page<Project> findAllByOwner(User owner, Pageable pageable);

    Page<Project> findAllByOwnerAndTitleContainingIgnoreCase(User owner, String title, Pageable pageable);

    Page<Project> findAllByTitleContainingIgnoreCase(String title, Pageable pageable);

    long countByOwner(User owner);
}
