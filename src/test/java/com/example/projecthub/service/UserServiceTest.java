package com.example.projecthub.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.projecthub.dto.RegistrationForm;
import com.example.projecthub.entity.Role;
import com.example.projecthub.entity.User;
import com.example.projecthub.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    UserRepository userRepository;

    @Mock
    PasswordEncoder passwordEncoder;

    @InjectMocks
    UserService userService;

    private RegistrationForm form;

    @BeforeEach
    void setUp() {
        form = new RegistrationForm();
        form.setLogin("ivan");
        form.setPassword("secret123");
        form.setPasswordConfirm("secret123");
    }

    @Test
    void registerCreatesUserWithBcryptHashAndUserRole() {
        when(userRepository.existsByLogin("ivan")).thenReturn(false);
        when(passwordEncoder.encode("secret123")).thenReturn("$2a$bcrypt-hash");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User saved = userService.register(form);

        assertThat(saved.getLogin()).isEqualTo("ivan");
        assertThat(saved.getPasswordHash()).isEqualTo("$2a$bcrypt-hash");
        assertThat(saved.getRole()).isEqualTo(Role.USER);
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    void registerRejectsMismatchedPasswords() {
        form.setPasswordConfirm("other");

        assertThatThrownBy(() -> userService.register(form))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("совпадают");

        verify(userRepository, never()).save(any());
    }

    @Test
    void registerRejectsDuplicateLogin() {
        when(userRepository.existsByLogin("ivan")).thenReturn(true);

        assertThatThrownBy(() -> userService.register(form))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Логин уже занят");
    }

    @Test
    void changeRoleUpdatesRole() {
        User existing = new User("ivan", "hash", Role.USER);
        existing.setId(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User updated = userService.changeRole(1L, Role.ADMIN);

        assertThat(updated.getRole()).isEqualTo(Role.ADMIN);
    }

    @Test
    void loadUserByUsernameReturnsAuthorities() {
        User u = new User("admin", "hash", Role.ADMIN);
        when(userRepository.findByLogin("admin")).thenReturn(Optional.of(u));

        var details = userService.loadUserByUsername("admin");

        assertThat(details.getUsername()).isEqualTo("admin");
        assertThat(details.getPassword()).isEqualTo("hash");
        assertThat(details.getAuthorities())
                .extracting(ga -> ga.getAuthority())
                .containsExactly("ROLE_ADMIN");
    }

    @Test
    void createUserSkipsIfAlreadyExists() {
        User existing = new User("admin", "hash", Role.ADMIN);
        when(userRepository.existsByLogin("admin")).thenReturn(true);
        when(userRepository.findByLogin("admin")).thenReturn(Optional.of(existing));

        User result = userService.createUser("admin", "anything", Role.USER);

        assertThat(result).isSameAs(existing);
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any());
    }
}
