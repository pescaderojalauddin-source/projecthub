package com.example.projecthub.service;

import com.example.projecthub.repository.UserRepository;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

/**
 * Подсвечивает @упоминания пользователей в тексте комментариев.
 *
 * <p>Алгоритм:
 * <ul>
 *   <li>Сначала весь текст экранируется (HTML-escape — на случай попадания
 *       внутрь {@code th:utext}).</li>
 *   <li>Регуляркой ищется паттерн {@code @login}; если такой логин существует
 *       в {@link com.example.projecthub.entity.User} — оборачиваем в
 *       {@code <a class="mention" href="/profile/{login}">@login</a>}.</li>
 *   <li>Несуществующие логины остаются обычным текстом.</li>
 * </ul>
 *
 * <p>Логины кэшируются на инстанс при первом обращении и пересчитываются
 * не чаще раза в 30 секунд — это исключает запрос в БД на каждый рендер
 * комментария. Тесты могут вызвать {@link #invalidate()} напрямую.
 */
@Service("mentionsService")
public class MentionsService {

    /** {@code @} + допустимые символы логина: латиница/цифры/точка/подчёркивание/дефис. */
    private static final Pattern MENTION_RE =
            Pattern.compile("(?<![\\p{L}\\p{N}_])@([A-Za-z0-9._-]{2,64})");

    private static final long TTL_NANOS = java.util.concurrent.TimeUnit.SECONDS.toNanos(30);

    private final UserRepository userRepository;
    private volatile Set<String> cachedLogins = Set.of();
    private volatile long cachedAtNanos = 0L;

    public MentionsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Возвращает HTML-фрагмент с подсвеченными @-меншенами. {@code null}/пустой
     * текст → пустая строка.
     */
    public String render(String text) {
        if (text == null || text.isBlank()) return "";
        Set<String> logins = loadLogins();
        String escaped = htmlEscape(text);

        Matcher m = MENTION_RE.matcher(escaped);
        StringBuilder out = new StringBuilder(escaped.length() + 32);
        int last = 0;
        while (m.find()) {
            String login = m.group(1);
            out.append(escaped, last, m.start());
            if (logins.contains(login.toLowerCase())) {
                out.append("<a class=\"mention\" href=\"/profile/").append(login).append("\">")
                   .append("@").append(login).append("</a>");
            } else {
                out.append(m.group()); // оставляем как есть
            }
            last = m.end();
        }
        out.append(escaped, last, escaped.length());

        // Сохраняем переносы строк (комментарии часто многострочные).
        return out.toString().replace("\n", "<br/>");
    }

    /** Сбрасывает кэш логинов (для тестов или после регистрации нового пользователя). */
    public void invalidate() {
        this.cachedAtNanos = 0L;
        this.cachedLogins = Set.of();
    }

    private Set<String> loadLogins() {
        long now = System.nanoTime();
        if (now - cachedAtNanos < TTL_NANOS && !cachedLogins.isEmpty()) {
            return cachedLogins;
        }
        Set<String> set = new HashSet<>();
        userRepository.findAll().forEach(u -> set.add(u.getLogin().toLowerCase()));
        this.cachedLogins = Set.copyOf(set);
        this.cachedAtNanos = now;
        return cachedLogins;
    }

    private static String htmlEscape(String s) {
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
