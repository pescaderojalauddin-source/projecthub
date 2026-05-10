package com.example.projecthub.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

/** Запись о разблокированной пользователем ачивке. */
@Entity
@Table(name = "achievements_unlocked")
public class AchievementUnlocked {

    @EmbeddedId
    private AchievementUnlockedId id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("userId")
    @JoinColumn(name = "user_id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User user;

    @Column(name = "unlocked_at", nullable = false)
    private LocalDateTime unlockedAt;

    public AchievementUnlocked() {
    }

    public AchievementUnlocked(User user, String code) {
        this.user = user;
        this.id = new AchievementUnlockedId(user.getId(), code);
        this.unlockedAt = LocalDateTime.now();
    }

    public AchievementUnlockedId getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public String getCode() {
        return id == null ? null : id.getCode();
    }

    public LocalDateTime getUnlockedAt() {
        return unlockedAt;
    }

    @Embeddable
    public static class AchievementUnlockedId implements Serializable {
        @Column(name = "user_id")
        private Long userId;
        @Column(name = "code", length = 64)
        private String code;

        public AchievementUnlockedId() {
        }

        public AchievementUnlockedId(Long userId, String code) {
            this.userId = userId;
            this.code = code;
        }

        public Long getUserId() {
            return userId;
        }

        public String getCode() {
            return code;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof AchievementUnlockedId other)) return false;
            return Objects.equals(userId, other.userId) && Objects.equals(code, other.code);
        }

        @Override
        public int hashCode() {
            return Objects.hash(userId, code);
        }
    }
}
