package com.hong.ForPaw.core.security;

import com.auth0.jwt.exceptions.JWTDecodeException;
import com.auth0.jwt.exceptions.SignatureVerificationException;
import com.auth0.jwt.exceptions.TokenExpiredException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.hong.ForPaw.core.utils.CookieUtils;
import com.hong.ForPaw.core.utils.LogUtils;
import com.hong.ForPaw.domain.User.UserRole;
import com.hong.ForPaw.domain.User.User;
import com.hong.ForPaw.service.RedisService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;


import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Enumeration;

@Slf4j
public class JwtAuthenticationFilter extends BasicAuthenticationFilter {

    private final RedisService redisService;

    public JwtAuthenticationFilter(AuthenticationManager authenticationManager, RedisService redisService) {
        super(authenticationManager);
        this.redisService = redisService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws IOException, ServletException {
        // LogUtils.logAllHeaders(request);

        // 엑세스 토큰은 '헤더'에서 추출
        String authorizationHeader = request.getHeader(JWTProvider.AUTHORIZATION);
        String accessToken = (authorizationHeader != null && authorizationHeader.startsWith(JWTProvider.TOKEN_PREFIX)) ?
                authorizationHeader.replace(JWTProvider.TOKEN_PREFIX, "") : null;

        // 리프레쉬 토큰은 '쿠키'에서 추출
        String refreshToken = CookieUtils.getCookieFromRequest(JWTProvider.REFRESH_TOKEN_COOKIE_KEY, request);

        // 1st 토큰 값이 존재하는지 체크
        if (checkIsTokenEmpty(accessToken, refreshToken)){
            chain.doFilter(request, response);
            return;
        }

        // 2nd 엑세스 토큰 검증
        User user = authenticateAccessToken(accessToken);

        // 3rd 엑세스 토큰에 인증 정보가 없음 => 리프레쉬 토큰이라도 날아온다면 사용해서 검증
        if (user == null) {
            user = authenticateRefreshToken(refreshToken);

            // 액세스 토큰과 리프레시 토큰 갱신 (재발급 로직)
            /*if (user != null) {
                accessToken = JWTProvider.createAccessToken(user);
                refreshToken = JWTProvider.createRefreshToken(user);

                updateToken(user, accessToken, refreshToken);
                CookieUtils.setCookieToResponse(JWTProvider.ACCESS_TOKEN_COOKIE_KEY, accessToken, JWTProvider.ACCESS_EXP_SEC, true, false, response);
                CookieUtils.setCookieToResponse(JWTProvider.REFRESH_TOKEN_COOKIE_KEY, refreshToken, JWTProvider.REFRESH_EXP_SEC, true, true, response);
            }*/
        }

        // 엑세스 토큰과 리프레쉬 토큰 모두 검증 실패 (만료 됐거나 잘못된 형식)
        if (user == null) {
            chain.doFilter(request, response);
            return;
        }

        // 권한 부여 (로그인된 상태)
        authenticateUser(user);
        redisService.addSetElement(createVisitKey(), user.getId());

        // Request 쿠키와 Response 쿠키 동기화
        CookieUtils.syncHttpResponseCookiesFromHttpRequest(request, response, JWTProvider.ACCESS_TOKEN_COOKIE_KEY, JWTProvider.REFRESH_TOKEN_COOKIE_KEY);

        // 엑세스 토큰은 HTTP Header로 리턴
        // response.setHeader(HttpHeaders.AUTHORIZATION, JWTProvider.TOKEN_PREFIX + accessToken);

        chain.doFilter(request, response);
    }

    private void authenticateUser(User user) {
        CustomUserDetails myUserDetails = new CustomUserDetails(user);
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                myUserDetails,
                myUserDetails.getPassword(),
                myUserDetails.getAuthorities()
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private User getUserFromToken(String token) {
        try {
            DecodedJWT decodedJWT = JWTProvider.verify(token);
            Long id = decodedJWT.getClaim("id").asLong();
            UserRole userRole = decodedJWT.getClaim("role").as(UserRole.class);
            String nickName = decodedJWT.getClaim("nickName").asString();
            return User.builder().id(id).role(userRole).nickName(nickName).build();
        } catch (SignatureVerificationException sve) {
            log.error("토큰 검증 실패");
        } catch (TokenExpiredException tee) {
            log.error("토큰 만료됨");
        } catch (JWTDecodeException jde) {
            log.error("잘못된 형태의 토큰값이 입력으로 들어와서 디코딩 실패");
        }
        return null;
    }

    private String createVisitKey() {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH");

        return "visit" + ":" + now.format(formatter);
    }

    private boolean checkIsTokenEmpty(String accessToken, String refreshToken) {
        return (accessToken == null || accessToken.trim().isEmpty()) && (refreshToken == null || refreshToken.trim().isEmpty());
    }

    private User authenticateAccessToken(String accessToken) {
        if (accessToken != null) {
            return getUserFromToken(accessToken);
        }
        return null;
    }

    private User authenticateRefreshToken(String refreshToken) {
        if (refreshToken != null) {
            User user = getUserFromToken(refreshToken);

            if (user != null && redisService.validateValue("refreshToken", String.valueOf(user.getId()), refreshToken)) {
                return user;
            }
        }
        return null;
    }

    private void updateToken(User user, String accessToken, String refreshToken){
        // 리프레쉬 토큰 갱신
        redisService.storeValue("refreshToken", String.valueOf(user.getId()), refreshToken, JWTProvider.REFRESH_EXP_MILLI);

        // 엑세스 토큰 갱신
        redisService.storeValue("accessToken", String.valueOf(user.getId()), accessToken, JWTProvider.ACCESS_EXP_MILLI);
    }
}