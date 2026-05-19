package com.example.projecthub.security;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.HandlerMapping;

// тесты на rate-limiter: после исчерпания лимита POST /login/POST /register
class RateLimitingFilterTest {

    @Test
    void allowsRequestsBelowLoginLimit() throws ServletException, IOException {
        RateLimitingFilter filter = new RateLimitingFilter();
        for (int i = 0; i < RateLimitingFilter.LOGIN_RPM; i++) {
            MockHttpServletResponse response = new MockHttpServletResponse();
            filter.doFilter(loginRequest("10.0.0.1"), response, passThrough());
            assertThat(response.getStatus()).as("attempt %d", i).isEqualTo(200);
        }
    }

    @Test
    void rejectsLoginOverLimitWith429() throws ServletException, IOException {
        RateLimitingFilter filter = new RateLimitingFilter();
        for (int i = 0; i < RateLimitingFilter.LOGIN_RPM; i++) {
            filter.doFilter(loginRequest("10.0.0.2"), new MockHttpServletResponse(), passThrough());
        }
        MockHttpServletResponse blocked = new MockHttpServletResponse();
        filter.doFilter(loginRequest("10.0.0.2"), blocked, passThrough());

        assertThat(blocked.getStatus()).isEqualTo(429);
        assertThat(blocked.getHeader("Retry-After")).isEqualTo("60");
        assertThat(blocked.getContentAsString()).contains("Too Many Requests");
    }

    @Test
    void buckersAreIsolatedPerIp() throws ServletException, IOException {
        RateLimitingFilter filter = new RateLimitingFilter();
        for (int i = 0; i < RateLimitingFilter.LOGIN_RPM; i++) {
            filter.doFilter(loginRequest("10.0.0.3"), new MockHttpServletResponse(), passThrough());
        }
        MockHttpServletResponse otherIp = new MockHttpServletResponse();
        filter.doFilter(loginRequest("10.0.0.4"), otherIp, passThrough());
        assertThat(otherIp.getStatus()).isEqualTo(200);
    }

    @Test
    void rejectsRegisterOverLimit() throws ServletException, IOException {
        RateLimitingFilter filter = new RateLimitingFilter();
        for (int i = 0; i < RateLimitingFilter.REGISTER_RPM; i++) {
            filter.doFilter(registerRequest("10.0.0.5"), new MockHttpServletResponse(), passThrough());
        }
        MockHttpServletResponse blocked = new MockHttpServletResponse();
        filter.doFilter(registerRequest("10.0.0.5"), blocked, passThrough());
        assertThat(blocked.getStatus()).isEqualTo(429);
    }

    @Test
    void getRequestsAreNotRateLimited() throws ServletException, IOException {
        RateLimitingFilter filter = new RateLimitingFilter();
    // гораздо больше запросов, чем лимиты; GET /login не должен ограничиваться
        for (int i = 0; i < 50; i++) {
            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/login");
            request.setRemoteAddr("10.0.0.6");
            request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "/login");
            MockHttpServletResponse response = new MockHttpServletResponse();
            filter.doFilter(request, response, passThrough());
            assertThat(response.getStatus()).isEqualTo(200);
        }
    }

    @Test
    void usesXForwardedForWhenPresent() throws ServletException, IOException {
        RateLimitingFilter filter = new RateLimitingFilter();
    // все запросы приходят с одного proxy IP, но X-Forwarded-For разный — лимиты не пересекаются
        for (int i = 0; i < RateLimitingFilter.LOGIN_RPM; i++) {
            MockHttpServletRequest request = loginRequest("127.0.0.1");
            request.addHeader("X-Forwarded-For", "203.0.113.10");
            filter.doFilter(request, new MockHttpServletResponse(), passThrough());
        }
        MockHttpServletRequest other = loginRequest("127.0.0.1");
        other.addHeader("X-Forwarded-For", "203.0.113.20");
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(other, response, passThrough());
        assertThat(response.getStatus()).isEqualTo(200);
    }

    private static MockHttpServletRequest loginRequest(String ip) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/login");
        request.setRemoteAddr(ip);
        return request;
    }

    private static MockHttpServletRequest registerRequest(String ip) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/register");
        request.setRemoteAddr(ip);
        return request;
    }

    private static FilterChain passThrough() {
        return (req, res) -> {
    // no-op — фильтр пропустил запрос дальше; статус по умолчанию 200
        };
    }
}
