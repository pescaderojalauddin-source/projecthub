package com.example.projecthub.repository;

import com.example.projecthub.entity.Project;
import com.example.projecthub.entity.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

// репозиторий проектов
public interface ProjectRepository extends JpaRepository<Project, Long> {

    // постраничный список проектов конкретного владельца
    @EntityGraph(attributePaths = "owner")
    Page<Project> findAllByOwner(User owner, Pageable pageable);

    // постраничный поиск по подстроке заголовка в проектах конкретного владельца
    @EntityGraph(attributePaths = "owner")
    Page<Project> findAllByOwnerAndTitleContainingIgnoreCase(User owner, String title, Pageable pageable);

    // постраничный поиск по подстроке заголовка во всех проектах (для ADMIN)
    @EntityGraph(attributePaths = "owner")
    Page<Project> findAllByTitleContainingIgnoreCase(String title, Pageable pageable);

    // подсчёт количества проектов конкретного владельца
    long countByOwner(User owner);

    // базовая постраничная выборка для ADMIN: тоже подгружаем владельца сразу
    @Override
    @EntityGraph(attributePaths = "owner")
    Page<Project> findAll(Pageable pageable);

    // сингл-выборка с жадной подгрузкой владельца
    @Override
    @EntityGraph(attributePaths = "owner")
    Optional<Project> findById(Long id);

    // глобальный поиск по подстроке названия/описания проекта (для ADMIN)
    @EntityGraph(attributePaths = "owner")
    @Query("""
            SELECT p FROM Project p
            WHERE LOWER(p.title) LIKE LOWER(CONCAT('%', :q, '%'))
               OR LOWER(p.description) LIKE LOWER(CONCAT('%', :q, '%'))
            ORDER BY p.id DESC
            """)
    List<Project> searchByText(String q, Pageable pageable);

    // глобальный поиск только в проектах данного владельца (для USER)
    @EntityGraph(attributePaths = "owner")
    @Query("""
            SELECT p FROM Project p
            WHERE p.owner = :owner
              AND (LOWER(p.title) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(p.description) LIKE LOWER(CONCAT('%', :q, '%')))
            ORDER BY p.id DESC
            """)
    List<Project> searchByTextForOwner(String q, User owner, Pageable pageable);
}
