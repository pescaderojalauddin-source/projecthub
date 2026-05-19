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

// рендер md в безопасный html для описаний проектов и задач
// рез-т идёт в th:utext, поэтому ОБЯЗАТЕЛЬНО через jsoup-sanitiser
@Service("markdownService")
public class MarkdownService {

    private final Parser parser;
    private final HtmlRenderer renderer;
    private final Safelist safelist;

    public MarkdownService() {
        List<Extension> extensions = List.of(AutolinkExtension.create());
        this.parser = Parser.builder().extensions(extensions).build();
        this.renderer = HtmlRenderer.builder().extensions(extensions).build();

    // базовый whitelist + заголовки. без img/script/iframe/style
        this.safelist = Safelist.basicWithImages()
                .removeTags("img")            // картинки только из локальной статики, не из md
                .addTags("h1", "h2", "h3", "h4", "h5", "h6")
                .addAttributes("a", "rel", "target");
    }

    // преобразовать markdown-текст в безопасный HTML. null/пустая строка → пустая строка
    public String render(String markdown) {
        if (markdown == null || markdown.isBlank()) {
            return "";
        }
        Node doc = parser.parse(markdown);
        String html = renderer.render(doc);
    // sanitiser принимает baseUri — он нужен, чтобы разрешать относительные ссылки
        String safe = Jsoup.clean(html, "/", safelist);
    // все внешние ссылки открываем в новой вкладке
        return safe.replace("<a href", "<a target=\"_blank\" rel=\"noopener noreferrer nofollow\" href");
    }
}
