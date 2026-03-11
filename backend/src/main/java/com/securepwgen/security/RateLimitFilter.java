package com.securepwgen.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Simple sliding-window rate limiter.
 * 30 requests per 60 seconds per IP address.
 * No external dependencies required.
 */
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);

    private static final int    MAX_REQUESTS    = 30;
    private static final long   WINDOW_MS       = 60_000; // 60 seconds

    // IP -> [requestCount, windowStart]
    private final ConcurrentHashMap<String, long[]> counters = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain)
            throws ServletException, IOException {

        String path = request.getRequestURI();
        if (!path.startsWith("/api/")) {
            chain.doFilter(request, response);
            return;
        }

        String ip  = resolveIp(request);
        long   now = Instant.now().toEpochMilli();

        long[] state = counters.compute(ip, (k, v) -> {
            if (v == null || (now - v[1]) > WINDOW_MS) {
                return new long[]{ 1, now };   // new window
            }
            v[0]++;
            return v;
        });

        long remaining = Math.max(0, MAX_REQUESTS - state[0]);
        response.setHeader("X-RateLimit-Remaining", String.valueOf(remaining));
        response.setHeader("X-RateLimit-Limit",     String.valueOf(MAX_REQUESTS));

        if (state[0] > MAX_REQUESTS) {
            log.warn("Rate limit exceeded for IP: {}", ip);
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setHeader("Retry-After", "60");
            response.getWriter().write(objectMapper.writeValueAsString(Map.of(
                "status",  429,
                "error",   "Too Many Requests",
                "message", "Rate limit exceeded. Max 30 requests/minute.",
                "retryAfterSeconds", 60
            )));
            return;
        }

        chain.doFilter(request, response);
    }

    private String resolveIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
