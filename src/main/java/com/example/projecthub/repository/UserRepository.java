package com.example.projecthub.repository;

import com.example.projecthub.entity.User;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Репозиторий пользователей. Используется для аутентификации (см. {@code UserService#loadUserByUsername})
 * и в админ-разделе.
 */
public interface UserRepository extends JpaRepository<User, Long> {

    /** Поиск пользователя по уникальному логину. */
    Optional<User> findByLogin(String login);

    /** Проверка занятости логина — используется в регистрации. */
    boolean existsByLogin(String login);

    /** Постраничный поиск пользователей по подстроке логина без учёта регистра. */
    Page<User> findAllByLoginContainingIgnoreCase(String login, Pageable pageable);
}
