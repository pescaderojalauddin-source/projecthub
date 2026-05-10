package com.example.projecthub.web;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import com.example.projecthub.repository.ProjectStarRepository;
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

    @Autowired
    ProjectStarRepository starRepository;

    User owner;
    Project project;

    @BeforeEach
    void seed() {
        starRepository.deleteAll();
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

    @Test
    void starButtonAppearsOnProjectList() throws Exception {
        mockMvc.perform(get("/projects").with(user("qw-owner").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("star-btn")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("/projects/" + project.getId() + "/star")));
    }

    @Test
    void toggleStarTwiceAddsAndRemovesFavourite() throws Exception {
        // Первый POST — добавили в избранное.
        mockMvc.perform(post("/projects/{id}/star", project.getId())
                        .with(user("qw-owner").roles("USER"))
                        .with(csrf())
                        .header("HX-Request", "true"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("bi-star-fill")));

        org.assertj.core.api.Assertions.assertThat(
                        starRepository.existsByUserAndProject(owner, project))
                .isTrue();

        // Второй POST — убрали.
        mockMvc.perform(post("/projects/{id}/star", project.getId())
                        .with(user("qw-owner").roles("USER"))
                        .with(csrf())
                        .header("HX-Request", "true"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("bi-star-fill"))));

        org.assertj.core.api.Assertions.assertThat(
                        starRepository.existsByUserAndProject(owner, project))
                .isFalse();
    }

    @Test
    void dashboardListsFavouritesAfterStarring() throws Exception {
        mockMvc.perform(post("/projects/{id}/star", project.getId())
                        .with(user("qw-owner").roles("USER"))
                        .with(csrf())
                        .header("HX-Request", "true"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/dashboard").with(user("qw-owner").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(model().attribute("favouritesTotal", 1))
                .andExpect(content().string(
                        org.hamcrest.Matchers.containsString("QuickWin Project")));
    }

    @Test
    void dashboardRendersChartsWithData() throws Exception {
        mockMvc.perform(get("/dashboard").with(user("qw-owner").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("id=\"statusChart\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("id=\"doneChart\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("data-todo=\"1\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("data-done=\"1\"")));
    }

    @Test
    void calendarRendersWithTaskForToday() throws Exception {
        mockMvc.perform(get("/projects/{id}/calendar", project.getId())
                        .with(user("qw-owner").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(view().name("projects/calendar"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("deadline-calendar")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Due today")));
    }

    // ==========================================================================
    // v3-features: cheatsheet, emoji, markdown, achievements
    // ==========================================================================

    @Test
    void cheatsheetModalRenderedOnEveryPage() throws Exception {
        mockMvc.perform(get("/dashboard").with(user("qw-owner").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("id=\"hotkeysCheatsheet\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("hotkeys.js")));
    }

    @Test
    void projectEmojiPersistsAndRendersOnViewPage() throws Exception {
        project.setEmoji("🚀");
        projectRepository.save(project);

        mockMvc.perform(get("/projects/{id}", project.getId())
                        .with(user("qw-owner").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("🚀")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("project-emoji")));
    }

    @Test
    void markdownInDescriptionRendersAsHtml() throws Exception {
        project.setDescription("# Hi\n**bold** and `code` and [link](https://example.com)");
        projectRepository.save(project);

        mockMvc.perform(get("/projects/{id}", project.getId())
                        .with(user("qw-owner").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("<strong>bold</strong>")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("<code>code</code>")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("target=\"_blank\"")));
    }

    @Test
    void markdownStripsScriptTags() throws Exception {
        project.setDescription("text <script>alert('xss')</script> end");
        projectRepository.save(project);

        mockMvc.perform(get("/projects/{id}", project.getId())
                        .with(user("qw-owner").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("<script>alert"))));
    }

    @Test
    void achievementsUnlockOnDashboardForUserWithDoneTask() throws Exception {
        // seed() уже создал 1 DONE → должна разблокироваться FIRST_DONE.
        mockMvc.perform(get("/dashboard").with(user("qw-owner").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("achievements"))
                .andExpect(model().attributeExists("achievementsUnlocked"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("achievement-grid")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Первая задача")));
    }
}
