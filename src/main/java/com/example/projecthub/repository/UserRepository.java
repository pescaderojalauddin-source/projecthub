package com.example.projecthub.repository;

import com.example.projecthub.entity.User;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByLogin(String login);

    boolean existsByLogin(String login);

    Page<User> findAllByLoginContainingIgnoreCase(String login, Pageable pageable);
}
