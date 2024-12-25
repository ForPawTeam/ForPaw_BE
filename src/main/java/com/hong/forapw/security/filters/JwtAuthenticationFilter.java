package com.hong.forapw.security.filters;

import com.auth0.jwt.exceptions.JWTDecodeException;
import com.auth0.jwt.exceptions.SignatureVerificationException;
import com.auth0.jwt.exceptions.TokenExpiredException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.hong.forapw.common.utils.CookieUtils;
import com.hong.forapw.domain.user.constant.UserRole;
import com.hong.forapw.domain.user.entity.User;
import com.hong.forapw.integration.redis.RedisService;
import com.hong.forapw.security.CustomUserDetails;
import com.hong.forapw.common.utils.JwtUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;


import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

import static com.hong.forapw.common.constants.GlobalConstants.*;
import static com.hong.forapw.common.utils.DateTimeUtils.DATE_HOUR_FORMAT;
import static com.hong.forapw.common.utils.DateTimeUtils.formatLocalDateTime;

@Slf4j
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
        String accessToken = extractAccessToken(request);
        String refreshToken = extractRefreshToken(request);

        if (areTokensEmpty(accessToken, refreshToken)) {
            chain.doFilter(request, response);
            return;
        }

        User user = authenticateWithTokens(accessToken, refreshToken);
        if (user == null) {
            chain.doFilter(request, response);
            return;
        }

        authenticateUser(user);
        recordUserVisit(user);
        syncRequestResponseCookies(request, response);

        chain.doFilter(request, response);
    }

    private String extractAccessToken(HttpServletRequest request) {
        String jwt = request.getHeader(AUTHORIZATION_HEADER);

        if (jwt != null && jwt.startsWith(BEARER_PREFIX)) {
            return removeTokenPrefix(jwt);
        }
        return null;
    }

    private String extractRefreshToken(HttpServletRequest request) {
        return CookieUtils.getFromRequest(REFRESH_TOKEN_KEY, request);
    }

    private boolean areTokensEmpty(String accessToken, String refreshToken) {
        return (accessToken == null || accessToken.trim().isEmpty()) &&
                (refreshToken == null || refreshToken.trim().isEmpty());
    }

    private User authenticateWithTokens(String accessToken, String refreshToken) {
        return Optional.ofNullable(authenticateAccessToken(accessToken))
                .orElseGet(() -> authenticateRefreshToken(refreshToken));
    }

    private User authenticateAccessToken(String accessToken) {
        return Optional.ofNullable(accessToken)
                .flatMap(jwtUtils::getUserFromToken)
                .orElse(null);
    }

    private User authenticateRefreshToken(String refreshToken) {
        return Optional.ofNullable(refreshToken)
                .flatMap(jwtUtils::getUserFromToken)
                .filter(user -> isRefreshTokenValid(user, refreshToken))
                .orElse(null);
    }

    private boolean isRefreshTokenValid(User user, String refreshToken) {
        return redisService.doesValueMatch(REFRESH_TOKEN_KEY, String.valueOf(user.getId()), refreshToken);
    }

    private void authenticateUser(User user) {
        CustomUserDetails myUserDetails = new CustomUserDetails(user);
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                myUserDetails,
                null,
                myUserDetails.getAuthorities()
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private void recordUserVisit(User user) {
        String visitKey = "visit" + ":" + formatLocalDateTime(LocalDateTime.now(), DATE_HOUR_FORMAT);
        redisService.addSetElement(visitKey, user.getId());
    }

    private void syncRequestResponseCookies(HttpServletRequest request, HttpServletResponse response) {
        Set<String> excludedCookies = Set.of(ACCESS_TOKEN_KEY, REFRESH_TOKEN_KEY);
        CookieUtils.syncRequestCookiesToResponse(request, response, excludedCookies);
    }

    private String removeTokenPrefix(String jwt) {
        return jwt.replace(BEARER_PREFIX, "");
    }
}