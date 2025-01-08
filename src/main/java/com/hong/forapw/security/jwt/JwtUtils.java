package com.hong.forapw.security.jwt;

import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTDecodeException;
import com.auth0.jwt.exceptions.SignatureVerificationException;
import com.auth0.jwt.exceptions.TokenExpiredException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.hong.forapw.domain.user.constant.UserRole;
import com.hong.forapw.domain.user.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import com.auth0.jwt.JWT;

import java.util.Date;
import java.util.Optional;

import static com.hong.forapw.common.constants.GlobalConstants.BEARER_PREFIX;
import static com.hong.forapw.common.constants.GlobalConstants.REFRESH_TOKEN_KEY;

@Component
@Slf4j
public class JwtUtils {

    @Value("${jwt.access-exp-milli}")
    public Long accessTokenExpirationMillis; // 1시간

    @Value("${jwt.refresh-exp-milli}")
    public Long refreshTokenExpirationMillis; // 일주일

    @Value("${jwt.refresh-exp-milli}")
    public Long refreshTokenExpirationSeconds;

    @Value("${jwt.secret}")
    public String secret;

    public String generateRefreshTokenCookie(String refreshToken) {
        return buildCookie(REFRESH_TOKEN_KEY, refreshToken, refreshTokenExpirationSeconds);
    }

    public String generateAccessToken(User user) {
        return generateToken(user, accessTokenExpirationMillis);
    }

    public String generateRefreshTokenCookie(User user) {
        return generateToken(user, refreshTokenExpirationMillis);
    }

    public Optional<User> getUserFromToken(String token) {
        try {
            String encodedJWT = removeTokenPrefix(token);
            DecodedJWT decodedJWT = decodeJWT(encodedJWT);

            Long id = decodedJWT.getClaim("id").asLong();
            String role = decodedJWT.getClaim("role").asString(); // ROLE_ 접두어 포함된 역할
            String nickname = decodedJWT.getClaim("nickName").asString();

            return Optional.of(User.builder().id(id).role(UserRole.valueOf(role.replace("ROLE_", ""))).nickName(nickname).build());
        } catch (Exception e) {
            logTokenException(token, e);
        }
        return Optional.empty();
    }

    public Optional<Long> getUserIdFromToken(String token) {
        try {
            DecodedJWT decodedJWT = decodeJWT(token);
            return Optional.of(decodedJWT.getClaim("id").asLong());
        } catch (Exception e) {
            logTokenException(token, e);
        }
        return Optional.empty();
    }

    private String buildCookie(String key, String value, Long maxAgeSeconds) {
        return ResponseCookie.from(key, value)
                .httpOnly(true)
                .secure(true)
                .path("/")
                .sameSite("Lax")
                .maxAge(maxAgeSeconds)
                .build()
                .toString();
    }

    private String generateToken(User user, Long exp) {
        return JWT.create()
                .withSubject(user.getEmail())
                .withExpiresAt(new Date(System.currentTimeMillis() + exp))
                .withClaim("id", user.getId())
                .withClaim("role", "ROLE_" + user.getRole()) // 역할에 ROLE_ 접두어 추가
                .withClaim("nickName", user.getNickname())
                .sign(Algorithm.HMAC512(secret));
    }

    private DecodedJWT decodeJWT(String encodedJWT) throws SignatureVerificationException, TokenExpiredException {
        return JWT.require(Algorithm.HMAC512(secret))
                .build()
                .verify(encodedJWT);
    }

    private String removeTokenPrefix(String jwt) {
        return jwt.replace(BEARER_PREFIX, "");
    }

    private void logTokenException(String token, Exception e) {
        if (e instanceof SignatureVerificationException) {
            log.error("토큰 유효성 검증 실패 {}: {}", token, e.getMessage());
        } else if (e instanceof TokenExpiredException) {
            log.error("토큰이 만료 되었음 {}: {}", token, e.getMessage());
        } else if (e instanceof JWTDecodeException) {
            log.error("토큰 디코딩 실패 {}: {}", token, e.getMessage());
        }
    }
}