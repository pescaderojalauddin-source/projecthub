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
    private final AuditService auditService;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       AuditService auditService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
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

    @Transactional(readOnly = true)
    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден: id=" + id));
    }

    @Transactional(readOnly = true)
    public User findByLogin(String login) {
        return userRepository.findByLogin(login)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден: " + login));
    }

    @Transactional(readOnly = true)
    public Optional<User> findOptionalByLogin(String login) {
        return userRepository.findByLogin(login);
    }

    @Transactional(readOnly = true)
    public List<User> findAll() {
        return userRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Page<User> search(String loginQuery, Pageable pageable) {
        if (loginQuery == null || loginQuery.isBlank()) {
            return userRepository.findAll(pageable);
        }
        return userRepository.findAllByLoginContainingIgnoreCase(loginQuery.trim(), pageable);
    }

    /** Меняет роль пользователя. Доступно только админу. */
    public User changeRole(Long userId, Role newRole) {
        User user = findById(userId);
        Role oldRole = user.getRole();
        user.setRole(newRole);
        User saved = userRepository.save(user);
        log.info("Пользователю id={} назначена роль {}", userId, newRole);
        auditService.record("ROLE_CHANGED", "User", userId, oldRole + " -> " + newRole);
        return saved;
    }

    /**
     * Меняет пароль пользователя при условии корректности текущего пароля и совпадения подтверждения.
     *
     * @throws IllegalArgumentException при несоответствии пароля или подтверждения
     */
    public void changePassword(User user, String currentPassword, String newPassword, String newPasswordConfirm) {
        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new IllegalArgumentException("Текущий пароль неверный");
        }
        if (!newPassword.equals(newPasswordConfirm)) {
            throw new IllegalArgumentException("Новый пароль и подтверждение не совпадают");
        }
        if (passwordEncoder.matches(newPassword, user.getPasswordHash())) {
            throw new IllegalArgumentException("Новый пароль совпадает с текущим");
        }
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        log.info("Пользователь id={} сменил пароль", user.getId());
        auditService.record("PASSWORD_CHANGED", "User", user.getId(), null);
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String login) throws UsernameNotFoundException {
        User user = userRepository.findByLogin(login)
                .orElseThrow(() -> new UsernameNotFoundException("Пользователь не найден: " + login));
        boolean accountNonLocked = !user.isAccountLocked();
        return org.springframework.security.core.userdetails.User
                .withUsername(user.getLogin())
                .password(user.getPasswordHash())
                .authorities(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
                .accountLocked(!accountNonLocked)
                .build();
    }
}
