package com.example.projecthub.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.projecthub.entity.Project;
import com.example.projecthub.entity.ProjectStar;
import com.example.projecthub.entity.ProjectStatus;
import com.example.projecthub.entity.Role;
import com.example.projecthub.entity.User;
import com.example.projecthub.repository.ProjectStarRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

// toggle ⭐ — два состояния, чекаем оба
@ExtendWith(MockitoExtension.class)
class ProjectStarServiceTest {

    @Mock ProjectStarRepository starRepository;
    @Mock ProjectService projectService;

    @InjectMocks ProjectStarService svc;

    User user;
    Project project;

    @BeforeEach
    void setUp() {
        user = new User("ivan", "h", Role.USER);
        user.setId(1L);
        project = new Project("P", "d", ProjectStatus.ACTIVE, user);
        project.setId(10L);
    }

    @Test
    void toggleAddsStarWhenNotPresent() {
        when(projectService.getByIdForUser(10L, user)).thenReturn(project);
        when(starRepository.existsByUserAndProject(user, project)).thenReturn(false);

        boolean now = svc.toggle(10L, user);

        assertThat(now).isTrue();
        verify(starRepository).save(any(ProjectStar.class));
    }

    @Test
    void toggleRemovesStarWhenPresent() {
        when(projectService.getByIdForUser(10L, user)).thenReturn(project);
        when(starRepository.existsByUserAndProject(user, project)).thenReturn(true);

        boolean now = svc.toggle(10L, user);

        assertThat(now).isFalse();
        verify(starRepository).deleteByUserAndProject(user, project);
    }

    @Test
    void countDelegatesToRepository() {
        when(starRepository.countByUser(user)).thenReturn(7L);
        assertThat(svc.countFavourites(user)).isEqualTo(7L);
    }
}
