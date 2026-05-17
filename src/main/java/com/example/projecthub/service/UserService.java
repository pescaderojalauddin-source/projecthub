package com.example.projecthub.service;

import com.example.projecthub.dto.RegistrationForm;
import com.example.projecthub.entity.Role;
import com.example.projecthub.entity.User;
import com.example.projecthub.exception.ResourceNotFoundException;
import com.example.projecthub.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Сервис пользователей. Реализует {@link UserDetailsService} для Spring Security.
 */
@Service
@Transactional
public class UserService implements UserDetailsService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /** Регистрация пользователя с ролью USER. */
    public User register(RegistrationForm form) {
        if (!form.getPassword().equals(form.getPasswordConfirm())) {
            throw new IllegalArgumentException("Пароли не совпадают");
        }
        if (userRepository.existsByLogin(form.getLogin())) {
            throw new IllegalArgumentException("Логин уже занят: " + form.getLogin());
        }
        User user = new User(form.getLogin(), passwordEncoder.encode(form.getPassword()), Role.USER);
        User saved = userRepository.save(user);
        log.info("Зарегистрирован пользователь id={} login={}", saved.getId(), saved.getLogin());
        return saved;
    }

    /** Создание пользователя «вручную» — для сидера демо-данных. */
    public User createUser(String login, String rawPassword, Role role) {
        if (userRepository.existsByLogin(login)) {
            return userRepository.findByLogin(login).orElseThrow();
        }
        User user = new User(login, passwordEncoder.encode(rawPassword), role);
        return userRepository.save(user);
    }

    /** Находит пользователя по ID или бросает {@link ResourceNotFoundException}. */
    @Transactional(readOnly = true)
    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден: id=" + id));
    }

    /** Находит пользователя по логину или бросает {@link ResourceNotFoundException}. */
    @Transactional(readOnly = true)
    public User findByLogin(String login) {
        return userRepository.findByLogin(login)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден: " + login));
    }

    /** Безопасный поиск пользователя по логину. */
    @Transactional(readOnly = true)
    public Optional<User> findOptionalByLogin(String login) {
        return userRepository.findByLogin(login);
    }

    /** Список всех пользователей — используется в dropdown'ах назначения исполнителя. */
    @Transactional(readOnly = true)
    public List<User> findAll() {
        return userRepository.findAll();
    }

    /** Постраничный поиск пользователей по подстроке логина. Доступно только АДМИНам. */
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public Page<User> search(String loginQuery, Pageable pageable) {
        if (loginQuery == null || loginQuery.isBlank()) {
            return userRepository.findAll(pageable);
        }
        return userRepository.findAllByLoginContainingIgnoreCase(loginQuery.trim(), pageable);
    }

    /** Меняет роль пользователя. Доступно только админу (защита продублирована на контроллере и на сервисе). */
    @PreAuthorize("hasRole('ADMIN')")
    public User changeRole(Long userId, Role newRole) {
        User user = findById(userId);
        user.setRole(newRole);
        log.info("Пользователю id={} назначена роль {}", userId, newRole);
        return userRepository.save(user);
    }

    /** Обновляет настройки email-уведомлений текущего пользователя. */
    public User updateNotificationSettings(User user, String email, boolean emailNotifications) {
        user.setEmail((email == null || email.isBlank()) ? null : email.trim());
        user.setEmailNotifications(emailNotifications);
        return userRepository.save(user);
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String login) throws UsernameNotFoundException {
        User user = userRepository.findByLogin(login)
                .orElseThrow(() -> new UsernameNotFoundException("Пользователь не найден: " + login));
        return new org.springframework.security.core.userdetails.User(
                user.getLogin(),
                user.getPasswordHash(),
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())));
    }
}
