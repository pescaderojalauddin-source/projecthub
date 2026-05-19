package com.example.projecthub.web;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.projecthub.entity.Role;
import com.example.projecthub.repository.UserRepository;
import com.example.projecthub.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

// интеграционные тесты основных HTTP-сценариев: login страница, регистрация, защита
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthFlowIT {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    UserService userService;

    @Autowired
    UserRepository userRepository;

    @BeforeEach
    void cleanUsers() {
        userRepository.deleteAll();
    }

    @Test
    void loginPageIsPublic() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("ProjectHub")));
    }

    @Test
    void projectsRequireAuthentication() throws Exception {
        mockMvc.perform(get("/projects"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    void registerCreatesUserAndRedirectsToLogin() throws Exception {
        mockMvc.perform(post("/register")
                        .with(csrf())
                        .param("login", "newuser")
                        .param("password", "secret123")
                        .param("passwordConfirm", "secret123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void registerRejectsBadInput() throws Exception {
        mockMvc.perform(post("/register")
                        .with(csrf())
                        .param("login", "ab")
                        .param("password", "x")
                        .param("passwordConfirm", "y"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Логин")));
    }

    @Test
    void adminPagesForbiddenForRegularUser() throws Exception {
        userService.createUser("plain", "user123", Role.USER);

        mockMvc.perform(get("/admin/users").with(user("plain").roles("USER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminPagesAccessibleForAdmin() throws Exception {
        userService.createUser("root", "admin123", Role.ADMIN);

        mockMvc.perform(get("/admin/users").with(user("root").roles("ADMIN")))
                .andExpect(status().isOk());
    }
}
