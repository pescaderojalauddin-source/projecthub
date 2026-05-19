package com.example.projecthub.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.example.projecthub.entity.Project;
import com.example.projecthub.entity.ProjectStatus;
import com.example.projecthub.entity.Role;
import com.example.projecthub.entity.Task;
import com.example.projecthub.entity.TaskStatus;
import com.example.projecthub.entity.User;
import com.example.projecthub.repository.CommentRepository;
import com.example.projecthub.repository.ProjectRepository;
import com.example.projecthub.repository.TaskRepository;
import com.example.projecthub.repository.UserRepository;
import com.example.projecthub.service.UserService;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

// интеграционные тесты UX-фич: канбан-доска, история задачи (Hibernate Envers), двойная
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UxFeaturesIT {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    UserService userService;

    @Autowired
    UserRepository userRepository;

    @Autowired
    ProjectRepository projectRepository;

    @Autowired
    TaskRepository taskRepository;

    @Autowired
    CommentRepository commentRepository;

    User owner;
    Project project;
    Task taskTodo;
    Task taskInProgress;

    @BeforeEach
    void seed() {
        commentRepository.deleteAll();
        taskRepository.deleteAll();
        projectRepository.deleteAll();
        userRepository.deleteAll();

        owner = userService.createUser("ux-owner", "secret123", Role.USER);
        project = projectRepository.save(
                new Project("UX Project", "kanban + history", ProjectStatus.ACTIVE, owner));
        taskTodo = taskRepository.save(
                new Task("First", "todo task", TaskStatus.TODO, LocalDate.now().plusDays(7), project, owner));
        taskInProgress = taskRepository.save(
                new Task("Second", "wip task", TaskStatus.IN_PROGRESS, LocalDate.now().plusDays(3), project, owner));
    }

    // ========================================================================
    // kANBAN BOARD
    // ========================================================================

    @Test
    void kanbanBoardGroupsTasksByStatus() throws Exception {
        mockMvc.perform(get("/projects/{id}/board", project.getId())
                        .with(user("ux-owner").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(view().name("projects/board"))
                .andExpect(model().attributeExists("tasksByStatus"))
                .andExpect(model().attribute("project", org.hamcrest.Matchers.hasProperty("id", org.hamcrest.Matchers.equalTo(project.getId()))))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("kanban-board")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("First")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Second")));
    }

    @Test
    void kanbanBoardForbiddenForUnrelatedUser() throws Exception {
        userService.createUser("intruder", "secret123", Role.USER);
        mockMvc.perform(get("/projects/{id}/board", project.getId())
                        .with(user("intruder").roles("USER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void kanbanBoardLoadsSortableAndHtmxAssets() throws Exception {
        mockMvc.perform(get("/projects/{id}/board", project.getId())
                        .with(user("ux-owner").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Sortable.min.js")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("htmx.min.js")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("/js/kanban.js")));
    }

    // ========================================================================
    // sTATUS CHANGE: HTMX vs обычная форма
    // ========================================================================

    @Test
    void statusChangeViaHtmxReturns204() throws Exception {
        mockMvc.perform(post("/tasks/{id}/status", taskTodo.getId())
                        .with(csrf())
                        .with(user("ux-owner").roles("USER"))
                        .header("HX-Request", "true")
                        .param("status", "DONE"))
                .andExpect(status().isNoContent());

        Task reloaded = taskRepository.findById(taskTodo.getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(TaskStatus.DONE);
    }

    @Test
    void statusChangeViaPlainFormRedirects() throws Exception {
        mockMvc.perform(post("/tasks/{id}/status", taskInProgress.getId())
                        .with(csrf())
                        .with(user("ux-owner").roles("USER"))
                        .param("status", "DONE"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/tasks/" + taskInProgress.getId()));

        Task reloaded = taskRepository.findById(taskInProgress.getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(TaskStatus.DONE);
    }

    // ========================================================================
    // hISTORY (Envers)
    // ========================================================================

    @Test
    void historyPageShowsAllRevisionsAfterEdits() throws Exception {
    // 3 раунда правок задачи: каждая создаёт новую ревизию в Envers
    // перезагружаем после каждого save — каждый saveAndFlush идёт в своёй транзакции
    // и оставляет локальный референс detached с устаревшим @Version
        Task t = taskRepository.findById(taskTodo.getId()).orElseThrow();
        t.setStatus(TaskStatus.IN_PROGRESS);
        taskRepository.saveAndFlush(t);

        t = taskRepository.findById(taskTodo.getId()).orElseThrow();
        t.setTitle("Renamed");
        taskRepository.saveAndFlush(t);

        t = taskRepository.findById(taskTodo.getId()).orElseThrow();
        t.setStatus(TaskStatus.DONE);
        taskRepository.saveAndFlush(t);

        mockMvc.perform(get("/tasks/{id}/history", taskTodo.getId())
                        .with(user("ux-owner").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(view().name("tasks/history"))
                .andExpect(model().attributeExists("revisions"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Renamed")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("История")));
    }

    @Test
    void historyForbiddenForUnrelatedUser() throws Exception {
        userService.createUser("nosey", "secret123", Role.USER);
        mockMvc.perform(get("/tasks/{id}/history", taskTodo.getId())
                        .with(user("nosey").roles("USER")))
                .andExpect(status().isForbidden());
    }

    // ========================================================================
    // dARK THEME
    // ========================================================================

    @Test
    void layoutEmbedsThemeToggleAndBootstrapVarsHook() throws Exception {
    // тёмная тема — это статика (localStorage + Bootstrap data-bs-theme)
    // достаточно убедиться, что в любом авторизованном шаблоне присутствуют:
    // 1) предзагрузочный скрипт, ставящий data-bs-theme до рендера,
    // 2) кнопка-переключатель,
    // 3) подключение theme.js
        mockMvc.perform(get("/projects").with(user("ux-owner").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("data-bs-theme")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("id=\"themeToggle\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("/js/theme.js")));
    }

    @Test
    void themeJsIsServedAsStaticResource() throws Exception {
        mockMvc.perform(get("/js/theme.js"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", org.hamcrest.Matchers.containsString("javascript")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("projecthub-theme")));
    }
}
