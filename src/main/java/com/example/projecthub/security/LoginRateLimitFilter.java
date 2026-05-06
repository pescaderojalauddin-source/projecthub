package com.example.projecthub.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Простой in-memory rate-limit на POST /login по IP клиента.
 * Лимит: {@value #MAX_REQUESTS_PER_WINDOW} попыток за {@value #WINDOW_SECONDS} секунд.
 * При превышении — 429 + редирект на {@code /login?ratelimit}.
 */
@Component
public class LoginRateLimitFilter extends OncePerRequestFilter {

    public static final int MAX_REQUESTS_PER_WINDOW = 10;
    public static final int WINDOW_SECONDS = 60;

    private static final Logger log = LoggerFactory.getLogger(LoginRateLimitFilter.class);

    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain)
            throws ServletException, IOException {
        if ("POST".equalsIgnoreCase(request.getMethod()) && "/login".equals(request.getRequestURI())) {
            String ip = clientIp(request);
            long now = System.currentTimeMillis();
            evictExpired(now);
            Window w = windows.compute(ip, (k, prev) -> {
                if (prev == null || prev.expired(now)) {
                    return new Window(now + WINDOW_SECONDS * 1000L);
                }
                return prev;
            });
            int count = w.count.incrementAndGet();
            if (count > MAX_REQUESTS_PER_WINDOW) {
                log.warn("Rate limit exceeded for ip={} count={}", ip, count);
                response.setStatus(429);
                response.sendRedirect("/login?ratelimit");
                return;
            }
        }
        chain.doFilter(request, response);
    }

    /**
     * Удаляет истёкшие окна, чтобы карта не росла бесконечно при распределённых атаках
     * с уникальных IP. Вызывается при каждом запросе на /login (т.е. не чаще нескольких раз в секунду).
     */
    private void evictExpired(long now) {
        windows.entrySet().removeIf(entry -> entry.getValue().expired(now));
    }

    private static String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private static final class Window {
        final long expiresAt;
        final AtomicInteger count = new AtomicInteger(0);

        Window(long expiresAt) {
            this.expiresAt = expiresAt;
        }

        boolean expired(long now) {
            return now >= expiresAt;
        }
    }
}
