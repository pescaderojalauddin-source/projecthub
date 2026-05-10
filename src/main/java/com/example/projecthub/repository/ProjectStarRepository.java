package com.example.projecthub.repository;

import com.example.projecthub.entity.Project;
import com.example.projecthub.entity.ProjectStar;
import com.example.projecthub.entity.User;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/**
 * Репозиторий «звёздочек» (избранные проекты пользователя).
 */
public interface ProjectStarRepository
        extends JpaRepository<ProjectStar, ProjectStar.ProjectStarId> {

    boolean existsByUserAndProject(User user, Project project);

    void deleteByUserAndProject(User user, Project project);

    /** Список избранных проектов пользователя (для дашборда и фильтра). */
    @EntityGraph(attributePaths = {"project", "project.owner"})
    List<ProjectStar> findByUserOrderByCreatedAtDesc(User user);

    long countByUser(User user);

    /** Множество id «звёздных» проектов пользователя — для отметки в списке. */
    @Query("""
            SELECT s.project.id FROM ProjectStar s
            WHERE s.user = :user AND s.project.id IN :projectIds
            """)
    Set<Long> findStarredProjectIds(User user, Collection<Long> projectIds);
}
