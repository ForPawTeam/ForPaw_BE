package com.hong.forapw.security.filters;

import com.hong.forapw.common.utils.CookieUtils;
import com.hong.forapw.domain.user.entity.User;
import com.hong.forapw.integration.redis.RedisService;
import com.hong.forapw.security.userdetails.CustomUserDetails;
import com.hong.forapw.security.jwt.JwtUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;


import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

import static com.hong.forapw.common.constants.GlobalConstants.*;
import static com.hong.forapw.common.utils.DateTimeUtils.DATE_HOUR_FORMAT;
import static com.hong.forapw.common.utils.DateTimeUtils.formatLocalDateTime;

@Slf4j
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final RedisService redisService;
    private final JwtUtils jwtUtils;

    public JwtAuthenticationFilter(RedisService redisService, JwtUtils jwtUtils) {
        this.redisService = redisService;
        this.jwtUtils = jwtUtils;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain) throws IOException, ServletException {
        String accessToken = extractAccessTokenFromHeader(request);
        if (isAccessTokenAbsent(accessToken)) {
            chain.doFilter(request, response);
            return;
        }

        User authenticatedUser = authenticateWithAccessToken(accessToken);
        if (authenticatedUser == null) {
            chain.doFilter(request, response);
            return;
        }

        setAuthenticationContext(authenticatedUser);
        recordUserVisit(authenticatedUser);
        syncCookiesBetweenRequestAndResponse(request, response);

        chain.doFilter(request, response);
    }

    private String extractAccessTokenFromHeader(HttpServletRequest request) {
        String authorizationHeader = request.getHeader(AUTHORIZATION_HEADER);
        if (authorizationHeader != null && authorizationHeader.startsWith(BEARER_PREFIX)) {
            return removeBearerPrefix(authorizationHeader);
        }
        return null;
    }

    private boolean isAccessTokenAbsent(String accessToken) {
        return accessToken == null || accessToken.trim().isEmpty();
    }

    private User authenticateWithAccessToken(String accessToken) {
        return Optional.ofNullable(accessToken)
                .flatMap(jwtUtils::getUserFromToken)
                .orElse(null);
    }

    private void setAuthenticationContext(User user) {
        CustomUserDetails userDetails = new CustomUserDetails(user);
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                userDetails.getAuthorities()
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private void recordUserVisit(User user) {
        String visitKey = "visit" + ":" + formatLocalDateTime(LocalDateTime.now(), DATE_HOUR_FORMAT);
        redisService.addSetElement(visitKey, user.getId());
    }

    private void syncCookiesBetweenRequestAndResponse(HttpServletRequest request, HttpServletResponse response) {
        Set<String> excludedCookies = Set.of(ACCESS_TOKEN_KEY, REFRESH_TOKEN_KEY);
        CookieUtils.syncRequestCookiesToResponse(request, response, excludedCookies);
    }

    private String removeBearerPrefix(String jwt) {
        return jwt.replace(BEARER_PREFIX, "");
    }
}