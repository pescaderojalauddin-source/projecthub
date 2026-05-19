package com.example.projecthub.web;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.projecthub.entity.Project;
import com.example.projecthub.entity.ProjectStatus;
import com.example.projecthub.entity.Role;
import com.example.projecthub.entity.User;
import com.example.projecthub.repository.CommentRepository;
import com.example.projecthub.repository.ProjectRepository;
import com.example.projecthub.repository.TaskRepository;
import com.example.projecthub.repository.UserRepository;
import com.example.projecthub.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

// интеграционные тесты REST API (/api/v1/**) аутентификация — HTTP Basic
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RestApiIT {

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
    ObjectMapper objectMapper;

    @BeforeEach
    void cleanData() {
        commentRepository.deleteAll();
        taskRepository.deleteAll();
        projectRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void unauthenticatedReturns401Json() throws Exception {
        mockMvc.perform(get("/api/v1/projects"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void userCanListAndCreateOwnProjects() throws Exception {
        userService.createUser("apiuser", "user123", Role.USER);

        mockMvc.perform(post("/api/v1/projects")
                        .with(httpBasic("apiuser", "user123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "title", "REST project",
                                "description", "via api",
                                "status", ProjectStatus.ACTIVE.name()))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.title").value("REST project"))
                .andExpect(jsonPath("$.ownerLogin").value("apiuser"));

        mockMvc.perform(get("/api/v1/projects").with(httpBasic("apiuser", "user123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("REST project"));
    }

    @Test
    void userCannotSeeOthersProject() throws Exception {
        User owner = userService.createUser("owner-rest", "user123", Role.USER);
        userService.createUser("intruder", "user123", Role.USER);
        Project project = projectRepository.save(
                new Project("Secret", "shh", ProjectStatus.ACTIVE, owner));

        mockMvc.perform(get("/api/v1/projects/" + project.getId())
                        .with(httpBasic("intruder", "user123")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void adminCanListUsers() throws Exception {
        userService.createUser("rest-admin", "admin123", Role.ADMIN);
        userService.createUser("plain1", "user123", Role.USER);

        mockMvc.perform(get("/api/v1/admin/users").with(httpBasic("rest-admin", "admin123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void userCannotAccessAdminApi() throws Exception {
        userService.createUser("plain2", "user123", Role.USER);

        mockMvc.perform(get("/api/v1/admin/users").with(httpBasic("plain2", "user123")))
                .andExpect(status().isForbidden());
    }

    @Test
    void getNonexistentReturns404Json() throws Exception {
        userService.createUser("any", "user123", Role.USER);
        mockMvc.perform(get("/api/v1/projects/99999").with(httpBasic("any", "user123")))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void userCanDeleteOwnProject() throws Exception {
        User owner = userService.createUser("del-owner", "user123", Role.USER);
        Project project = projectRepository.save(
                new Project("To delete", "x", ProjectStatus.ACTIVE, owner));

        mockMvc.perform(delete("/api/v1/projects/" + project.getId())
                        .with(httpBasic("del-owner", "user123")))
                .andExpect(status().isNoContent());
    }

    @Test
    void formLoginEndpointStillWorks() throws Exception {
        userService.createUser("ui-user", "user123", Role.USER);
        mockMvc.perform(formLogin("/login")
                        .userParameter("login")
                        .passwordParam("password")
                        .user("ui-user")
                        .password("user123"))
                .andExpect(status().is3xxRedirection());
    }
}
