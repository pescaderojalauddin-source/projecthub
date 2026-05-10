package com.example.projecthub.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.projecthub.config.JpaConfig;
import com.example.projecthub.entity.Project;
import com.example.projecthub.entity.ProjectStatus;
import com.example.projecthub.entity.Role;
import com.example.projecthub.entity.User;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;

/**
 * Проверяем JPA Auditing (@CreatedBy/@LastModifiedBy/@CreatedDate/@LastModifiedDate)
 * и оптимистичную блокировку через @Version на реальной БД (H2).
 */
@DataJpaTest(showSql = false)
@AutoConfigureTestDatabase
@Import(JpaConfig.class)
@ActiveProfiles("test")
class ProjectAuditingAndLockingTest {

    @Autowired
    UserRepository userRepository;

    @Autowired
    ProjectRepository projectRepository;

    @Autowired
    EntityManager em;

    private User owner;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "ivan", "n/a",
                        java.util.List.of(new SimpleGrantedAuthority("ROLE_USER"))));
        owner = userRepository.save(new User("ivan", "hash", Role.USER));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void auditingFieldsAreFilledOnInsert() {
        Project p = projectRepository.save(new Project("P", "d", ProjectStatus.ACTIVE, owner));

        assertThat(p.getCreatedAt()).isNotNull();
        assertThat(p.getUpdatedAt()).isNotNull();
        assertThat(p.getCreatedBy()).isEqualTo("ivan");
        assertThat(p.getUpdatedBy()).isEqualTo("ivan");
        assertThat(p.getVersion()).isEqualTo(0L);
    }

    @Test
    void auditingFieldsAreUpdatedOnSave() {
        Project p = projectRepository.save(new Project("P", "d", ProjectStatus.ACTIVE, owner));
        em.flush();
        em.clear();

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "admin", "n/a",
                        java.util.List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));

        Project loaded = projectRepository.findById(p.getId()).orElseThrow();
        loaded.setTitle("P-updated");
        Project saved = projectRepository.saveAndFlush(loaded);

        assertThat(saved.getCreatedBy()).isEqualTo("ivan"); // не меняется
        assertThat(saved.getUpdatedBy()).isEqualTo("admin"); // обновлён
        assertThat(saved.getVersion()).isEqualTo(1L); // @Version инкрементировался
    }

    @Test
    void optimisticLockingPreventsLostUpdate() {
        Project p = projectRepository.save(new Project("P", "d", ProjectStatus.ACTIVE, owner));
        em.flush();
        em.clear();

        // Загружаем копию №1, отсоединяем — это эмулирует первого «редактора».
        Project copy1 = projectRepository.findById(p.getId()).orElseThrow();
        em.detach(copy1);
        // Загружаем копию №2 заново — это второй «редактор» с тем же version=0.
        Project copy2 = projectRepository.findById(p.getId()).orElseThrow();
        em.detach(copy2);

        // copy1 сохраняется первой — версия становится 1.
        copy1.setTitle("by-user-1");
        projectRepository.saveAndFlush(copy1);
        em.clear();

        // copy2 со старой версией 0 пытается записать — должна получить optimistic lock failure.
        copy2.setTitle("by-user-2");
        assertThatThrownBy(() -> projectRepository.saveAndFlush(copy2))
                .isInstanceOf(ObjectOptimisticLockingFailureException.class);
    }

    @Test
    void auditorFallsBackToSystemWhenNoSecurityContext() {
        SecurityContextHolder.clearContext();
        Project p = projectRepository.save(new Project("S", "d", ProjectStatus.ACTIVE, owner));

        assertThat(p.getCreatedBy()).isEqualTo(JpaConfig.SYSTEM_AUDITOR);
        assertThat(p.getUpdatedBy()).isEqualTo(JpaConfig.SYSTEM_AUDITOR);
    }
}
