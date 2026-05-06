package com.example.projecthub.repository;

import com.example.projecthub.entity.Project;
import com.example.projecthub.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Репозиторий проектов. Содержит методы выборки, скоупированной по владельцу
 * (для USER), и общие постраничные выборки (для ADMIN).
 */
public interface ProjectRepository extends JpaRepository<Project, Long> {

    /** Постраничный список проектов конкретного владельца. */
    Page<Project> findAllByOwner(User owner, Pageable pageable);

    /** Постраничный поиск по подстроке заголовка в проектах конкретного владельца. */
    Page<Project> findAllByOwnerAndTitleContainingIgnoreCase(User owner, String title, Pageable pageable);

    /** Постраничный поиск по подстроке заголовка во всех проектах (для ADMIN). */
    Page<Project> findAllByTitleContainingIgnoreCase(String title, Pageable pageable);

    /** Подсчёт количества проектов конкретного владельца. */
    long countByOwner(User owner);
}
