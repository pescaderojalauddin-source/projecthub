package com.example.projecthub.web;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
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

/**
 * Интеграционные тесты «quick-win»-фич:
 * дашборд «Мой день», глобальный поиск и прогресс-бар проекта.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class QuickWinsIT {

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

    @BeforeEach
    void seed() {
        commentRepository.deleteAll();
        taskRepository.deleteAll();
        projectRepository.deleteAll();
        userRepository.deleteAll();

        owner = userService.createUser("qw-owner", "secret123", Role.USER);
        project = projectRepository.save(
                new Project("QuickWin Project", "ProGresS bar test",
                        ProjectStatus.ACTIVE, owner));
        // дедлайн сегодня, не завершено → попадает в «Сегодня» дашборда
        taskRepository.save(new Task("Due today", "x",
                TaskStatus.IN_PROGRESS, LocalDate.now(), project, owner));
        // просроченная, не завершена → в «Просрочено»
        taskRepository.save(new Task("Overdue", "x",
                TaskStatus.TODO, LocalDate.now().minusDays(2), project, owner));
        // готово → попадёт в счётчик «Готово»
        taskRepository.save(new Task("Done", "x",
                TaskStatus.DONE, LocalDate.now().minusDays(5), project, owner));
        // ещё одна в работе для прогресс-бара
        taskRepository.save(new Task("WIP", "x",
                TaskStatus.IN_PROGRESS, LocalDate.now().plusDays(3), project, owner));
    }

    @Test
    void dashboardShowsAssignedTasksGrouped() throws Exception {
        mockMvc.perform(get("/dashboard").with(user("qw-owner").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"))
                .andExpect(model().attribute("countDueToday", 1L))
                .andExpect(model().attribute("countOverdue", 1L))
                .andExpect(model().attribute("countInProgress", 2L))
                .andExpect(model().attribute("countDone", 1L))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("qw-owner")));
    }

    @Test
    void rootRedirectsAuthenticatedUserToDashboard() throws Exception {
        mockMvc.perform(get("/").with(user("qw-owner").roles("USER")))
                .andExpect(status().is3xxRedirection())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .redirectedUrl("/dashboard"));
    }

    @Test
    void searchReturnsProjectsAndTasksForOwner() throws Exception {
        mockMvc.perform(get("/search").param("q", "QuickWin")
                        .with(user("qw-owner").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("QuickWin Project")));
    }

    @Test
    void searchReturnsHintWhenQueryTooShort() throws Exception {
        mockMvc.perform(get("/search").param("q", "a")
                        .with(user("qw-owner").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("минимум")));
    }

    @Test
    void searchScopesResultsToOwnerForRegularUser() throws Exception {
        userService.createUser("stranger", "secret123", Role.USER);
        mockMvc.perform(get("/search").param("q", "QuickWin")
                        .with(user("stranger").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("QuickWin Project"))));
    }

    @Test
    void projectListIncludesProgressBar() throws Exception {
        mockMvc.perform(get("/projects").with(user("qw-owner").roles("USER")))
                .andExpect(status().isOk())
                // 1 готовая из 4 → 25%
                .andExpect(content().string(org.hamcrest.Matchers.containsString("width: 25%")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("25%")));
    }
}
