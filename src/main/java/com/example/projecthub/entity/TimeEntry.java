package com.example.projecthub.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Запись отслеживания рабочего времени по задаче.
 * Активная (незавершённая) запись имеет {@code endAt == null} и {@code durationSeconds == null}.
 */
@Entity
@Table(name = "time_entries")
public class TimeEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "task_id", nullable = false)
    private Task task;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "start_at", nullable = false)
    private LocalDateTime startAt;

    @Column(name = "end_at")
    private LocalDateTime endAt;

    @Column(name = "duration_seconds")
    private Long durationSeconds;

    @Column(name = "note", length = 500)
    private String note;

    public TimeEntry() {
    }

    public TimeEntry(Task task, User user, LocalDateTime startAt) {
        this.task = task;
        this.user = user;
        this.startAt = startAt;
    }

    /** Завершить запись и зафиксировать длительность. */
    public void stop(LocalDateTime endAt, String note) {
        this.endAt = endAt;
        this.durationSeconds = Math.max(0L, Duration.between(startAt, endAt).getSeconds());
        if (note != null && !note.isBlank()) {
            this.note = note.length() > 500 ? note.substring(0, 500) : note;
        }
    }

    public boolean isActive() {
        return endAt == null;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Task getTask() { return task; }
    public void setTask(Task task) { this.task = task; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public LocalDateTime getStartAt() { return startAt; }
    public void setStartAt(LocalDateTime startAt) { this.startAt = startAt; }
    public LocalDateTime getEndAt() { return endAt; }
    public void setEndAt(LocalDateTime endAt) { this.endAt = endAt; }
    public Long getDurationSeconds() { return durationSeconds; }
    public void setDurationSeconds(Long durationSeconds) { this.durationSeconds = durationSeconds; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}
