package com.hong.forapw.security.filters;

import com.hong.forapw.integration.redis.RedisService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitingFilter extends OncePerRequestFilter {

    private static final int MAX_REQUESTS = 1000; // 허용 요청 수 (REST API에 대한 DDOS 공격 방지 목적)
    private static final long TIME_WINDOW_SECONDS = 60; // 제한 시간 (초 단위)
    private static final String RATE_LIMIT_EXCEEDED_MESSAGE = "Too Many Requests";
    private static final String RATE_LIMIT_PREFIX = "rate_limit";

    private final RedisService redisService;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) {

        String clientIp = resolveClientIp(request);
        String rateLimitKey = buildRateLimitKey(clientIp);

        try {
            if (isRateLimitExceeded(rateLimitKey)) {
                log.warn("요청 제한 초과, IP: {}", clientIp);
                handleRateLimitExceeded(response);
                return;
            }

            filterChain.doFilter(request, response);
        } catch (Exception ex) {
            log.error("요청 제한 필터 처리 과정 중 에러 발생: ", ex);
        }
    }

    private boolean isRateLimitExceeded(String rateLimitKey) {
        try {
            Long requestCount = redisService.incrementAndGet(rateLimitKey, 1L);
            if (requestCount == 1) {
                redisService.setKeyExpiration(rateLimitKey, TIME_WINDOW_SECONDS);
            }
            return requestCount > MAX_REQUESTS;
        } catch (Exception e) {
            log.error("Redis 작업 중 오류 발생, 기본적으로 요청을 허용합니다. 키: {}", rateLimitKey, e);
            return false;
        }
    }

    private String resolveClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty()) {
            ip = request.getRemoteAddr();
        } else {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    private String buildRateLimitKey(String identifier) {
        return RATE_LIMIT_PREFIX + ":" + identifier;
    }

    private void handleRateLimitExceeded(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
        response.getWriter().write(RATE_LIMIT_EXCEEDED_MESSAGE);
    }
}