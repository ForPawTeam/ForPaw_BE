package com.hong.forapw.security.filters;

import com.hong.forapw.security.userdetails.CustomUserDetails;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

/**
 * 현재 프로젝트에 적용시키는 것은 시기상조라 판단하여 테스트만 해보고 적용은 하지 않는다.
 */
@Slf4j
public class TraceIdFilter extends OncePerRequestFilter {

    private static final String TRACE_ID_HEADER = "X-Trace-Id"; // 클라이언트와 서버 간 TraceId를 공유하기 위한 헤더 이름
    private static final String MDC_TRACE_ID_KEY = "traceId";
    private static final String MDC_USER_ID_KEY = "userId";

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, HttpServletResponse response,
                                    @NonNull FilterChain chain)
            throws IOException, ServletException {
        try {
            String traceId = getOrCreateTraceId(request);
            MDC.put(MDC_TRACE_ID_KEY, traceId);
            response.setHeader(TRACE_ID_HEADER, traceId);

            String authenticatedUserId = getAuthenticatedUserId();
            if (authenticatedUserId != null) {
                MDC.put(MDC_USER_ID_KEY, authenticatedUserId);
            }

            chain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_TRACE_ID_KEY);
            MDC.remove(MDC_USER_ID_KEY);
        }
    }

    private String getOrCreateTraceId(HttpServletRequest request) {
        String existingTraceId = request.getHeader(TRACE_ID_HEADER);
        if (existingTraceId == null || existingTraceId.isEmpty()) {
            String newTraceId = UUID.randomUUID().toString();
            log.debug("새 TraceId 생성: {}", newTraceId);
            return newTraceId;
        } else {
            log.debug("기존 TraceId 사용: {}", existingTraceId);
            return existingTraceId;
        }
    }

    private String getAuthenticatedUserId() {
        return Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
                .filter(Authentication::isAuthenticated)
                .map(Authentication::getPrincipal)
                .filter(CustomUserDetails.class::isInstance)
                .map(CustomUserDetails.class::cast)
                .map(userDetails -> String.valueOf(userDetails.user().getId()))
                .orElse(null);
    }
}