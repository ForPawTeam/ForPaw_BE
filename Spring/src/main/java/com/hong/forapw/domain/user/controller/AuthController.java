package com.hong.forapw.domain.user.controller;

import com.hong.forapw.common.utils.ApiUtils;
import com.hong.forapw.domain.user.model.LoginResult;
import com.hong.forapw.domain.user.model.request.LoginReq;
import com.hong.forapw.domain.user.model.response.LoginRes;
import com.hong.forapw.domain.user.service.AuthService;
import com.hong.forapw.security.jwt.JwtUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class AuthController {

    private final AuthService authService;
    private final JwtUtils jwtUtils;

    private static final String AUTH_KAKAO = "KAKAO";
    private static final String AUTH_GOOGLE = "GOOGLE";

    @PostMapping("/auth/login")
    public ResponseEntity<?> login(@RequestBody @Valid LoginReq request, HttpServletRequest servletRequest) {
        LoginResult loginResult = authService.login(request, servletRequest);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, jwtUtils.generateRefreshTokenCookie(loginResult.refreshToken()))
                .body(ApiUtils.success(HttpStatus.OK, new LoginRes(loginResult.accessToken())));
    }

    @GetMapping("/auth/login/kakao")
    public void loginWithKakao(@RequestParam String code, HttpServletRequest request, HttpServletResponse response) {
        LoginResult loginResult = authService.loginWithKakao(code, request);
        authService.redirectAfterOauthLogin(loginResult, AUTH_KAKAO, response);
    }

    @GetMapping("/auth/login/google")
    public void loginWithGoogle(@RequestParam String code, HttpServletRequest request, HttpServletResponse response) {
        LoginResult loginResult = authService.loginWithGoogle(code, request);
        authService.redirectAfterOauthLogin(loginResult, AUTH_GOOGLE, response);
    }
}
