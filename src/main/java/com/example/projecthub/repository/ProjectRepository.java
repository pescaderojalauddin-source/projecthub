package com.example.projecthub.repository;

import com.example.projecthub.entity.Project;
import com.example.projecthub.entity.User;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Репозиторий проектов. Содержит методы выборки, скоупированной по владельцу
 * (для USER), и общие постраничные выборки (для ADMIN).
 *
 * <p>Все методы списочных выборок используют {@link EntityGraph} для жадной
 * подгрузки {@code owner} — это исключает N+1 при рендеринге списка проектов
 * (Thymeleaf обращается к {@code project.owner.login}).
 */
public interface ProjectRepository extends JpaRepository<Project, Long> {

    /** Постраничный список проектов конкретного владельца. */
    @EntityGraph(attributePaths = "owner")
    Page<Project> findAllByOwner(User owner, Pageable pageable);

    /** Постраничный поиск по подстроке заголовка в проектах конкретного владельца. */
    @EntityGraph(attributePaths = "owner")
    Page<Project> findAllByOwnerAndTitleContainingIgnoreCase(User owner, String title, Pageable pageable);

    /** Постраничный поиск по подстроке заголовка во всех проектах (для ADMIN). */
    @EntityGraph(attributePaths = "owner")
    Page<Project> findAllByTitleContainingIgnoreCase(String title, Pageable pageable);

    /** Подсчёт количества проектов конкретного владельца. */
    long countByOwner(User owner);

    /** Базовая постраничная выборка для ADMIN: тоже подгружаем владельца сразу. */
    @Override
    @EntityGraph(attributePaths = "owner")
    Page<Project> findAll(Pageable pageable);

    /**
     * Сингл-выборка с жадной подгрузкой владельца. Используется для страницы проекта,
     * где в шаблоне обращаемся к {@code project.owner.login} уже после закрытия транзакции
     * (open-in-view=false).
     */
    @Override
    @EntityGraph(attributePaths = "owner")
    Optional<Project> findById(Long id);
}
