package com.example.projecthub.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;
import org.hibernate.envers.RelationTargetAuditMode;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * Задача внутри проекта. Имеет статус, дедлайн и исполнителя.
 *
 * <p>Аннотация {@link Audited} включает Hibernate Envers — каждая правка задачи
 * пишется в {@code tasks_aud} + {@code revinfo}, доступна через {@code TaskRepository.findRevisions()}.
 * Refs на {@code project} и {@code assignee} хранятся по id без жадного лога,
 * поэтому эти связанные сущности {@code @NotAudited}-эквиваленты.
 */
@Entity
@Table(name = "tasks")
@EntityListeners(AuditingEntityListener.class)
@Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "description", length = 4000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private TaskStatus status;

    /** Приоритет задачи. Дефолт — MEDIUM (см. миграцию V6). */
    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false, length = 16)
    private TaskPriority priority = TaskPriority.MEDIUM;

    @Column(name = "deadline")
    private LocalDate deadline;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignee_id")
    private User assignee;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @CreatedBy
    @Column(name = "created_by", length = 64, updatable = false)
    private String createdBy;

    @LastModifiedBy
    @Column(name = "updated_by", length = 64)
    private String updatedBy;

    /** Оптимистичная блокировка: предотвращает потерянные апдейты при одновременном редактировании. */
    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    /**
     * Комментарии к задаче. Inverse-сторона связи: владелец (task) — {@link Comment#getTask()}.
     * Не включается в audit-trail: коллекция меняется часто и имеет собственные события.
     */
    @NotAudited
    @OneToMany(mappedBy = "task", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Comment> comments = new ArrayList<>();

    /**
     * Теги задачи: simple value collection (строки), хранится в side-таблице task_tags.
     * Не audited — слишком шумно для Envers и не несёт исторической ценности.
     */
    @NotAudited
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "task_tags", joinColumns = @JoinColumn(name = "task_id"))
    @Column(name = "tag", length = 40, nullable = false)
    private Set<String> tags = new LinkedHashSet<>();

    /**
     * Файлы-вложения, прикреплённые к задаче.
     */
    @NotAudited
    @OneToMany(mappedBy = "task", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<TaskAttachment> attachments = new ArrayList<>();

    public Task() {
    }

    public Task(String title, String description, TaskStatus status, LocalDate deadline,
                Project project, User assignee) {
        this.title = title;
        this.description = description;
        this.status = status;
        this.deadline = deadline;
        this.project = project;
        this.assignee = assignee;
        this.createdAt = LocalDateTime.now(); // перезапишется @CreatedDate при persist.
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public void setStatus(TaskStatus status) {
        this.status = status;
    }

    public LocalDate getDeadline() {
        return deadline;
    }

    public void setDeadline(LocalDate deadline) {
        this.deadline = deadline;
    }

    public Project getProject() {
        return project;
    }

    public void setProject(Project project) {
        this.project = project;
    }

    public User getAssignee() {
        return assignee;
    }

    public void setAssignee(User assignee) {
        this.assignee = assignee;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public Long getVersion() {
        return version;
    }

    public List<Comment> getComments() {
        return comments;
    }

    public void setComments(List<Comment> comments) {
        this.comments = comments;
    }

    public TaskPriority getPriority() {
        return priority;
    }

    public void setPriority(TaskPriority priority) {
        this.priority = (priority != null) ? priority : TaskPriority.MEDIUM;
    }

    public Set<String> getTags() {
        return tags;
    }

    public void setTags(Set<String> tags) {
        this.tags = (tags != null) ? tags : new LinkedHashSet<>();
    }

    public List<TaskAttachment> getAttachments() {
        return attachments;
    }

    public void setAttachments(List<TaskAttachment> attachments) {
        this.attachments = attachments;
    }
}
