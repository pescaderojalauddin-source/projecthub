package com.example.projecthub.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.MapsId;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

/**
 * «Звёздочка» — пометка проекта как избранного пользователем.
 * Композитный ключ {@code (user_id, project_id)} обеспечивает уникальность.
 */
@Entity
@Table(name = "project_stars", indexes = {
        @Index(name = "idx_project_stars_user", columnList = "user_id"),
        @Index(name = "idx_project_stars_project", columnList = "project_id")
})
public class ProjectStar {

    @EmbeddedId
    private ProjectStarId id;

    @ManyToOne
    @MapsId("userId")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User user;

    @ManyToOne
    @MapsId("projectId")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Project project;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public ProjectStar() {
    }

    public ProjectStar(User user, Project project) {
        this.user = user;
        this.project = project;
        this.id = new ProjectStarId(user.getId(), project.getId());
        this.createdAt = LocalDateTime.now();
    }

    public ProjectStarId getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public Project getProject() {
        return project;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /** Композитный ключ: (user_id, project_id). */
    @Embeddable
    public static class ProjectStarId implements Serializable {

        @Column(name = "user_id")
        private Long userId;

        @Column(name = "project_id")
        private Long projectId;

        public ProjectStarId() {
        }

        public ProjectStarId(Long userId, Long projectId) {
            this.userId = userId;
            this.projectId = projectId;
        }

        public Long getUserId() {
            return userId;
        }

        public Long getProjectId() {
            return projectId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof ProjectStarId that)) return false;
            return Objects.equals(userId, that.userId)
                    && Objects.equals(projectId, that.projectId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(userId, projectId);
        }
    }
}
