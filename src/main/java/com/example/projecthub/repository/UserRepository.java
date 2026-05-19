package com.example.projecthub.repository;

import com.example.projecthub.entity.User;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

// репозиторий юзеров
public interface UserRepository extends JpaRepository<User, Long> {

    // поиск юзера по уникальному логину
    Optional<User> findByLogin(String login);

    // проверка занятости логина — используется в регистрации
    boolean existsByLogin(String login);

    // постраничный поиск юзеров по подстроке логина без учёта регистра
    Page<User> findAllByLoginContainingIgnoreCase(String login, Pageable pageable);
}
