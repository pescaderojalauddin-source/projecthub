package com.example.projecthub.service;

import java.util.List;
import org.commonmark.Extension;
import org.commonmark.ext.autolink.AutolinkExtension;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.stereotype.Service;

/**
 * Рендер Markdown в безопасный HTML для описаний проектов и задач.
 * <p>
 * Используется из Thymeleaf-шаблонов через {@code ${@markdownService.render(text)}} и
 * результат вставляется как {@code th:utext} (unescaped) — поэтому критически
 * важно прогонять отрендеренный HTML через jsoup-sanitiser.
 */
@Service("markdownService")
public class MarkdownService {

    private final Parser parser;
    private final HtmlRenderer renderer;
    private final Safelist safelist;

    public MarkdownService() {
        List<Extension> extensions = List.of(AutolinkExtension.create());
        this.parser = Parser.builder().extensions(extensions).build();
        this.renderer = HtmlRenderer.builder().extensions(extensions).build();

        // Базовый whitelist: только text-formatting + ссылки. Без img/script/iframe/style.
        this.safelist = Safelist.basicWithImages()
                .removeTags("img")            // картинки рендерим только из локальной статики, не из markdown
                .addAttributes("a", "rel", "target");
    }

    /** Преобразовать markdown-текст в безопасный HTML. {@code null}/пустая строка → пустая строка. */
    public String render(String markdown) {
        if (markdown == null || markdown.isBlank()) {
            return "";
        }
        Node doc = parser.parse(markdown);
        String html = renderer.render(doc);
        // Sanitiser принимает baseUri — он нужен, чтобы разрешать относительные ссылки.
        String safe = Jsoup.clean(html, "/", safelist);
        // Все внешние ссылки открываем в новой вкладке.
        return safe.replace("<a href", "<a target=\"_blank\" rel=\"noopener noreferrer nofollow\" href");
    }
}
