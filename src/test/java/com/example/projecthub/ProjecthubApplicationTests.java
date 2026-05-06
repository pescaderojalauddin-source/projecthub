package com.example.projecthub;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class ProjecthubApplicationTests {

    @Test
    void contextLoads() {
        // smoke-тест: контекст приложения должен подняться
    }
}
