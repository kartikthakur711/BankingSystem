package com.kartik.bankingsystem.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static class Counter {
        long windowStartMs;
        int count;
    }

    private final Map<String, Counter> counters = new ConcurrentHashMap<>();

    @Value("${app.rate-limit.login.max-requests:10}")
    private int loginMaxRequests;

    @Value("${app.rate-limit.login.window-ms:60000}")
    private long loginWindowMs;

    @Value("${app.rate-limit.transfer.max-requests:20}")
    private int transferMaxRequests;

    @Value("${app.rate-limit.transfer.window-ms:60000}")
    private long transferWindowMs;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();
        String method = request.getMethod();

        LimitConfig config = resolveConfig(path, method);
        if (config == null) {
            filterChain.doFilter(request, response);
            return;
        }

        String key = resolveClientKey(request) + ":" + config.keySuffix;
        long now = Instant.now().toEpochMilli();

        Counter counter = counters.computeIfAbsent(key, k -> {
            Counter c = new Counter();
            c.windowStartMs = now;
            c.count = 0;
            return c;
        });

        synchronized (counter) {
            if (now - counter.windowStartMs >= config.windowMs) {
                counter.windowStartMs = now;
                counter.count = 0;
            }

            counter.count++;
            if (counter.count > config.maxRequests) {
                response.setStatus(429);
                response.setContentType("application/json");
                response.getWriter().write("{\"message\":\"Rate limit exceeded. Try again later.\"}");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private String resolveClientKey(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private LimitConfig resolveConfig(String path, String method) {
        if ("POST".equalsIgnoreCase(method) && "/api/v1/auth/login".equals(path)) {
            return new LimitConfig("login", loginMaxRequests, loginWindowMs);
        }
        if ("POST".equalsIgnoreCase(method) && "/api/v1/accounts/transfer".equals(path)) {
            return new LimitConfig("transfer", transferMaxRequests, transferWindowMs);
        }
        return null;
    }

    private record LimitConfig(String keySuffix, int maxRequests, long windowMs) {}
}
