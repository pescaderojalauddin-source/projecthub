package com.example.projecthub.entity.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.envers.RevisionEntity;
import org.hibernate.envers.RevisionNumber;
import org.hibernate.envers.RevisionTimestamp;

/**
 * Кастомная сущность ревизий для Hibernate Envers.
 *
 * <p>Заменяет {@code DefaultRevisionEntity} ради явной стратегии генерации {@code IDENTITY}:
 * {@code DefaultRevisionEntity} использует {@code @GeneratedValue} без указания стратегии и
 * на PostgreSQL ожидает сиквенс {@code revinfo_seq}, которого в нашей миграции V3 нет.
 *
 * <p>Схема: см. {@code db/migration/V3__envers_audit_tables.sql} — колонки {@code rev} (PK,
 * identity) и {@code revtstmp}.
 */
@Entity
@RevisionEntity
@Table(name = "revinfo")
public class RevInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @RevisionNumber
    @Column(name = "rev")
    private int id;

    @RevisionTimestamp
    @Column(name = "revtstmp")
    private long timestamp;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
