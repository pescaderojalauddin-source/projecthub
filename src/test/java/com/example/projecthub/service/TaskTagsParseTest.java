package com.example.projecthub.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import org.junit.jupiter.api.Test;

// тесты на статич функ-ю TaskService.parseTags — без моков, чисто алгоритмика
class TaskTagsParseTest {

    @Test
    void nullAndBlankReturnEmptySet() {
        assertThat(TaskService.parseTags(null)).isEmpty();
        assertThat(TaskService.parseTags("")).isEmpty();
        assertThat(TaskService.parseTags("   ")).isEmpty();
    }

    @Test
    void splitsByCommaAndSpaces() {
        LinkedHashSet<String> tags = TaskService.parseTags("backend, api  ui frontend");
        assertThat(new ArrayList<>(tags))
                .containsExactly("backend", "api", "ui", "frontend");
    }

    @Test
    void lowercasesAllTags() {
        LinkedHashSet<String> tags = TaskService.parseTags("Backend, API");
        assertThat(new ArrayList<>(tags)).containsExactly("backend", "api");
    }

    @Test
    void deduplicatesIgnoringCase() {
        LinkedHashSet<String> tags = TaskService.parseTags("api, API, Api");
        assertThat(tags).containsExactly("api");
    }

    @Test
    void leadingHashIsStripped() {
        LinkedHashSet<String> tags = TaskService.parseTags("#backend, #frontend");
        assertThat(tags).containsExactly("backend", "frontend");
    }

    @Test
    void tooLongTagIsTruncatedToFortyChars() {
        String longTag = "a".repeat(60);
        LinkedHashSet<String> tags = TaskService.parseTags(longTag);
        assertThat(tags).hasSize(1);
        assertThat(tags.iterator().next()).hasSize(40);
    }

    @Test
    void preservesInsertionOrder() {
        LinkedHashSet<String> tags = TaskService.parseTags("z, a, m");
        assertThat(new ArrayList<>(tags)).containsExactly("z", "a", "m");
    }

    @Test
    void emptyTokensAreIgnored() {
        LinkedHashSet<String> tags = TaskService.parseTags("a, , , b");
        assertThat(tags).containsExactly("a", "b");
    }
}
