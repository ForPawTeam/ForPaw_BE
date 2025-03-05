package com.hong.forapw.auth;

import com.hong.forapw.common.utils.ApiUtils;
import com.hong.forapw.domain.user.model.LoginResult;
import com.hong.forapw.domain.user.model.request.LoginReq;
import com.hong.forapw.domain.user.model.response.LoginRes;
import com.hong.forapw.auth.oauth.SocialProvider;
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


    @PostMapping("/auth/login")
    public ResponseEntity<?> login(@RequestBody @Valid LoginReq request, HttpServletRequest servletRequest) {
        LoginResult loginResult = authService.login(request, servletRequest);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, jwtUtils.generateRefreshTokenCookie(loginResult.refreshToken()))
                .body(ApiUtils.success(HttpStatus.OK, new LoginRes(loginResult.accessToken())));
    }

    @GetMapping("/auth/login/{provider}")
    public void loginSocial(
            @PathVariable("provider") String providerStr,
            @RequestParam String code,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        SocialProvider provider = SocialProvider.valueOf(providerStr.toUpperCase());
        LoginResult loginResult = authService.loginWithSocial(code, provider, request);
        authService.redirectAfterOauthLogin(loginResult, provider.name(), response);
    }
}
