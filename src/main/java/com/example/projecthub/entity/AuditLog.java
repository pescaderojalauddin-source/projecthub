package com.example.projecthub.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/**
 * Запись журнала аудита: кто, когда и что сделал.
 * Заполняется через {@link com.example.projecthub.service.AuditService}.
 */
@Entity
@Table(name = "audit_log", indexes = {
        @Index(name = "idx_audit_log_created_at", columnList = "created_at"),
        @Index(name = "idx_audit_log_action", columnList = "action"),
        @Index(name = "idx_audit_log_actor", columnList = "actor")
})
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "action", nullable = false, length = 64)
    private String action;

    @Column(name = "actor", length = 64)
    private String actor;

    @Column(name = "target_type", length = 32)
    private String targetType;

    @Column(name = "target_id")
    private Long targetId;

    @Column(name = "details", length = 1000)
    private String details;

    @Column(name = "ip", length = 64)
    private String ip;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public AuditLog() {
    }

    public AuditLog(String action, String actor, String targetType, Long targetId,
                    String details, String ip) {
        this.action = action;
        this.actor = actor;
        this.targetType = targetType;
        this.targetId = targetId;
        this.details = details;
        this.ip = ip;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getAction() {
        return action;
    }

    public String getActor() {
        return actor;
    }

    public String getTargetType() {
        return targetType;
    }

    public Long getTargetId() {
        return targetId;
    }

    public String getDetails() {
        return details;
    }

    public String getIp() {
        return ip;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
