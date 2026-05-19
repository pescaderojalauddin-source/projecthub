package com.example.projecthub.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

// rate-limit для /login и /register на Bucket4j (in-memory, per IP)
// пороги: 5 попыток в минуту, иначе 429
@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitingFilter.class);

    // максимум запросов на POST /login в минуту с одного IP
    static final int LOGIN_RPM = 10;
    // максимум запросов на POST /register в минуту с одного IP
    static final int REGISTER_RPM = 5;

    private final ConcurrentMap<String, Bucket> loginBuckets = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Bucket> registerBuckets = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            chain.doFilter(request, response);
            return;
        }

        String path = request.getRequestURI();
        Bucket bucket = null;
        String limit = null;
        if ("/login".equals(path)) {
            bucket = loginBuckets.computeIfAbsent(clientIp(request), ip -> newBucket(LOGIN_RPM));
            limit = "login";
        } else if ("/register".equals(path)) {
            bucket = registerBuckets.computeIfAbsent(clientIp(request), ip -> newBucket(REGISTER_RPM));
            limit = "register";
        }

        if (bucket != null && !bucket.tryConsume(1)) {
            log.warn("Rate limit exceeded for {} from {}", limit, clientIp(request));
            response.setStatus(429);
            response.setHeader("Retry-After", "60");
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(
                    "{\"status\":429,\"error\":\"Too Many Requests\","
                            + "\"message\":\"Слишком много попыток, повторите позже.\"}");
            return;
        }

        chain.doFilter(request, response);
    }

    private static Bucket newBucket(int rpm) {
        return Bucket.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(rpm)
                        .refillIntervally(rpm, Duration.ofMinutes(1))
                        .build())
                .build();
    }

    private static String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
