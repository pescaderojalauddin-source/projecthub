package com.example.projecthub.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.projecthub.entity.Role;
import com.example.projecthub.entity.User;
import com.example.projecthub.repository.UserRepository;
import com.example.projecthub.service.AuditService;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.core.Authentication;

@ExtendWith(MockitoExtension.class)
class AuthEventListenerTest {

    @Mock
    UserRepository userRepository;

    @Mock
    AuditService auditService;

    @InjectMocks
    AuthEventListener listener;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User("ivan", "hash", Role.USER);
        user.setId(2L);
    }

    @Test
    void successResetsCounterAndAuditsLoginSuccess() {
        user.setFailedLoginAttempts(3);
        when(userRepository.findByLogin("ivan")).thenReturn(Optional.of(user));

        listener.onSuccess(new AuthenticationSuccessEvent(authToken("ivan")));

        assertThat(user.getFailedLoginAttempts()).isZero();
        assertThat(user.getLockedUntil()).isNull();
        verify(userRepository).save(user);
        verify(auditService).record(eq("LOGIN_SUCCESS"), eq("ivan"), any());
    }

    @Test
    void failureIncrementsCounterAndAuditsLoginFailure() {
        when(userRepository.findByLogin("ivan")).thenReturn(Optional.of(user));

        listener.onFailure(badCredEvent("ivan"));

        assertThat(user.getFailedLoginAttempts()).isEqualTo(1);
        assertThat(user.getLockedUntil()).isNull();
        verify(userRepository).save(user);
        verify(auditService).record(eq("LOGIN_FAILURE"), eq("ivan"), anyString());
        verify(auditService, never()).record(eq("ACCOUNT_LOCKED"), anyString(), anyString());
    }

    @Test
    void fifthFailureLocksAccountAndAuditsLock() {
        user.setFailedLoginAttempts(4);
        when(userRepository.findByLogin("ivan")).thenReturn(Optional.of(user));

        listener.onFailure(badCredEvent("ivan"));

        assertThat(user.getFailedLoginAttempts()).isEqualTo(5);
        assertThat(user.getLockedUntil()).isNotNull();
        assertThat(user.isAccountLocked()).isTrue();
        verify(auditService).record(eq("ACCOUNT_LOCKED"), eq("ivan"), anyString());
        verify(auditService, times(1)).record(eq("LOGIN_FAILURE"), eq("ivan"), anyString());
    }

    @Test
    void failureForUnknownLoginStillAuditsButDoesNotSave() {
        when(userRepository.findByLogin("ghost")).thenReturn(Optional.empty());

        listener.onFailure(badCredEvent("ghost"));

        verify(userRepository, never()).save(any());
        verify(auditService).record(eq("LOGIN_FAILURE"), eq("ghost"), anyString());
    }

    private static Authentication authToken(String login) {
        return new UsernamePasswordAuthenticationToken(login, "x");
    }

    private static AuthenticationFailureBadCredentialsEvent badCredEvent(String login) {
        return new AuthenticationFailureBadCredentialsEvent(
                authToken(login), new BadCredentialsException("bad"));
    }
}
