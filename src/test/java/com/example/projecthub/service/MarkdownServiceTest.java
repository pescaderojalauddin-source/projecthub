package com.example.projecthub.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

// чисто юнит — без спринга, инстанс через new()
class MarkdownServiceTest {

    private final MarkdownService svc = new MarkdownService();

    @Test
    void nullAndBlankReturnEmptyString() {
        assertThat(svc.render(null)).isEmpty();
        assertThat(svc.render("")).isEmpty();
        assertThat(svc.render("   \n\t  ")).isEmpty();
    }

    @Test
    void boldAndItalicAndInlineCodeAreRendered() {
        String html = svc.render("**жирный** *курсив* `код`");
        assertThat(html).contains("<strong>жирный</strong>");
        assertThat(html).contains("<em>курсив</em>");
        assertThat(html).contains("<code>код</code>");
    }

    @Test
    void headersAreRendered() {
        String html = svc.render("# h1\n## h2");
        assertThat(html).contains("<h1>h1</h1>");
        assertThat(html).contains("<h2>h2</h2>");
    }

    @Test
    void unorderedListIsRendered() {
        String html = svc.render("- a\n- b\n- c");
        assertThat(html).contains("<ul>");
        assertThat(html).contains("<li>a</li>");
    }

    @Test
    void externalLinksGetSafeAttributes() {
    // на ссылке должны прилететь target=_blank + noopener/noreferrer/nofollow
        String html = svc.render("[google](https://google.com)");
        assertThat(html).contains("href=\"https://google.com\"");
        assertThat(html).contains("target=\"_blank\"");
        assertThat(html).contains("rel=\"noopener noreferrer nofollow\"");
    }

    @Test
    void scriptTagsAreStrippedBySanitizer() {
        String html = svc.render("<script>alert('xss')</script>обычный текст");
        assertThat(html).doesNotContain("<script");
        assertThat(html).doesNotContain("alert('xss')");
        assertThat(html).contains("обычный текст");
    }

    @Test
    void iframeIsStripped() {
        String html = svc.render("<iframe src='http://evil'></iframe>");
        assertThat(html).doesNotContain("<iframe");
    }

    @Test
    void imageTagsAreStripped() {
    // картинки запрещены — чтоб не тащить внешние ресурсы
        String html = svc.render("![alt](http://example.com/x.png)");
        assertThat(html).doesNotContain("<img");
    }

    @Test
    void onErrorAttributeIsStripped() {
    // sanitizer должен резать опасные обработчики
        String html = svc.render("<a href=\"#\" onclick=\"alert(1)\">x</a>");
        assertThat(html).doesNotContain("onclick");
    }

    @Test
    void autolinkExtensionTurnsBarePlainUrlIntoAnchor() {
        String html = svc.render("see https://example.com here");
        assertThat(html).contains("<a");
        assertThat(html).contains("href=\"https://example.com\"");
    }

    @Test
    void codeBlockIsRendered() {
        String html = svc.render("```\nlet x = 1\n```");
        assertThat(html).contains("<pre>");
        assertThat(html).contains("<code>");
    }
}
