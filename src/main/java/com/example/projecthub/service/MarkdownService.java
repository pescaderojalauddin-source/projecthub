package com.example.projecthub.service;

import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.data.MutableDataSet;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.stereotype.Service;

/**
 * Конвертер Markdown → безопасный HTML для отображения в Thymeleaf через {@code th:utext}.
 *
 * <p>Поток: flexmark парсер → HtmlRenderer → jsoup-санитайзер с whitelist-правилами,
 * исключающими {@code <script>}, обработчики событий и javascript:-ссылки. Это защищает
 * от хранимого XSS даже если злоумышленник зарегистрируется и вставит вредоносный markdown.
 */
@Service
public class MarkdownService {

    private final Parser parser;
    private final HtmlRenderer renderer;
    private final Safelist safelist;

    public MarkdownService() {
        MutableDataSet options = new MutableDataSet();
        this.parser = Parser.builder(options).build();
        this.renderer = HtmlRenderer.builder(options).build();
        this.safelist = Safelist.basicWithImages()
                .addAttributes("a", "target")
                .addEnforcedAttribute("a", "rel", "nofollow noopener noreferrer");
    }

    /** Преобразует markdown-текст в безопасный HTML. {@code null} → пустая строка. */
    public String toSafeHtml(String markdown) {
        if (markdown == null || markdown.isBlank()) {
            return "";
        }
        String rawHtml = renderer.render(parser.parse(markdown));
        return Jsoup.clean(rawHtml, safelist);
    }
}
