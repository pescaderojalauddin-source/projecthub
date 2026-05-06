package com.example.projecthub.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.projecthub.dto.ProjectForm;
import com.example.projecthub.entity.Project;
import com.example.projecthub.entity.ProjectStatus;
import com.example.projecthub.entity.Role;
import com.example.projecthub.entity.User;
import com.example.projecthub.exception.AccessDeniedAppException;
import com.example.projecthub.exception.ResourceNotFoundException;
import com.example.projecthub.repository.ProjectRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

    @Mock
    ProjectRepository projectRepository;

    @InjectMocks
    ProjectService projectService;

    private User owner;
    private User stranger;
    private User admin;
    private Project project;

    @BeforeEach
    void setUp() {
        owner = new User("ivan", "h", Role.USER);
        owner.setId(1L);
        stranger = new User("maria", "h", Role.USER);
        stranger.setId(2L);
        admin = new User("admin", "h", Role.ADMIN);
        admin.setId(3L);

        project = new Project("P", "d", ProjectStatus.ACTIVE, owner);
        project.setId(10L);
    }

    @Test
    void createSavesProjectWithOwner() {
        ProjectForm f = new ProjectForm();
        f.setTitle("New project");
        f.setDescription("desc");
        f.setStatus(ProjectStatus.ACTIVE);
        when(projectRepository.save(any(Project.class))).thenAnswer(inv -> inv.getArgument(0));

        Project saved = projectService.create(f, owner);

        assertThat(saved.getTitle()).isEqualTo("New project");
        assertThat(saved.getOwner()).isSameAs(owner);
        assertThat(saved.getStatus()).isEqualTo(ProjectStatus.ACTIVE);
    }

    @Test
    void getByIdForOwnerReturnsProject() {
        when(projectRepository.findById(10L)).thenReturn(Optional.of(project));

        Project result = projectService.getByIdForUser(10L, owner);

        assertThat(result).isSameAs(project);
    }

    @Test
    void getByIdDeniesStranger() {
        when(projectRepository.findById(10L)).thenReturn(Optional.of(project));

        assertThatThrownBy(() -> projectService.getByIdForUser(10L, stranger))
                .isInstanceOf(AccessDeniedAppException.class);
    }

    @Test
    void getByIdAllowsAdmin() {
        when(projectRepository.findById(10L)).thenReturn(Optional.of(project));

        Project result = projectService.getByIdForUser(10L, admin);

        assertThat(result).isSameAs(project);
    }

    @Test
    void getByIdReturns404WhenMissing() {
        when(projectRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectService.getByIdForUser(99L, owner))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void listForUserCallsOwnerScopeForRegularUser() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Project> page = new PageImpl<>(java.util.List.of(project));
        when(projectRepository.findAllByOwner(eq(owner), eq(pageable))).thenReturn(page);

        Page<Project> result = projectService.listForUser(owner, null, pageable);

        assertThat(result.getContent()).containsExactly(project);
        verify(projectRepository, times(1)).findAllByOwner(owner, pageable);
    }

    @Test
    void listForUserCallsFindAllForAdmin() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Project> page = new PageImpl<>(java.util.List.of(project));
        when(projectRepository.findAll(eq(pageable))).thenReturn(page);

        Page<Project> result = projectService.listForUser(admin, null, pageable);

        assertThat(result.getContent()).containsExactly(project);
        verify(projectRepository).findAll(pageable);
    }

    @Test
    void deleteRejectsStranger() {
        when(projectRepository.findById(10L)).thenReturn(Optional.of(project));

        assertThatThrownBy(() -> projectService.delete(10L, stranger))
                .isInstanceOf(AccessDeniedAppException.class);
    }
}
