package com.example.projecthub.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class MarkdownServiceTest {

    private final MarkdownService service = new MarkdownService();

    @Test
    void rendersBasicFormatting() {
        String html = service.toSafeHtml("**bold** and *italic* and `code`.");
        assertThat(html).contains("<strong>bold</strong>");
        assertThat(html).contains("<em>italic</em>");
        assertThat(html).contains("<code>code</code>");
    }

    @Test
    void rendersListsAndLinks() {
        String html = service.toSafeHtml("- one\n- two\n\n[ProjectHub](https://example.com)");
        assertThat(html).contains("<ul>");
        assertThat(html).contains("<li>one</li>");
        assertThat(html).contains("<li>two</li>");
        assertThat(html).contains("href=\"https://example.com\"");
        assertThat(html).contains("rel=\"nofollow noopener noreferrer\"");
    }

    @Test
    void stripsScriptTags() {
        String html = service.toSafeHtml("Hello <script>alert('xss')</script> world");
        assertThat(html).doesNotContain("<script");
        assertThat(html).doesNotContain("alert");
        assertThat(html).contains("Hello");
        assertThat(html).contains("world");
    }

    @Test
    void stripsJavascriptUrls() {
        String html = service.toSafeHtml("[click](javascript:alert(1))");
        assertThat(html).doesNotContain("javascript:");
    }

    @Test
    void stripsOnEventHandlers() {
        String html = service.toSafeHtml("<img src=x onerror='alert(1)'>");
        assertThat(html).doesNotContain("onerror");
        assertThat(html).doesNotContain("alert");
    }

    @Test
    void emptyAndNullReturnEmpty() {
        assertThat(service.toSafeHtml(null)).isEmpty();
        assertThat(service.toSafeHtml("")).isEmpty();
        assertThat(service.toSafeHtml("   ")).isEmpty();
    }
}
