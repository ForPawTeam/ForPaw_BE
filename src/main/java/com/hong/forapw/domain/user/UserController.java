package com.hong.forapw.domain.user;

import com.hong.forapw.domain.user.model.LoginResult;
import com.hong.forapw.domain.user.model.TokenDTO;
import com.hong.forapw.domain.user.model.request.*;
import com.hong.forapw.domain.user.model.response.*;
import com.hong.forapw.integration.oauth.OAuthService;
import com.hong.forapw.security.userdetails.CustomUserDetails;
import com.hong.forapw.security.jwt.JwtUtils;
import com.hong.forapw.common.utils.ApiUtils;
import com.hong.forapw.domain.user.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class UserController {

    private final UserService userService;
    private final OAuthService oAuthService;
    private final JwtUtils jwtUtils;

    private static final String AUTH_KAKAO = "KAKAO";
    private static final String AUTH_GOOGLE = "GOOGLE";
    private static final String CODE_TYPE_JOIN = "join";
    private static final String CODE_TYPE_WITHDRAW = "withdraw";
    private static final String CODE_TYPE_RECOVERY = "recovery";

    @PostMapping("/auth/login")
    public ResponseEntity<?> login(@RequestBody @Valid LoginReq request, HttpServletRequest servletRequest) {
        LoginResult loginResult = userService.login(request, servletRequest);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, jwtUtils.generateRefreshTokenCookie(loginResult.refreshToken()))
                .body(ApiUtils.success(HttpStatus.OK, new LoginRes(loginResult.accessToken())));
    }

    @GetMapping("/auth/login/kakao")
    public void loginWithKakao(@RequestParam String code, HttpServletRequest request, HttpServletResponse response) {
        LoginResult loginResult = oAuthService.loginWithKakao(code, request);
        oAuthService.redirectAfterOAuthLogin(loginResult, AUTH_KAKAO, response);
    }

    @GetMapping("/auth/login/google")
    public void loginWithGoogle(@RequestParam String code, HttpServletRequest request, HttpServletResponse response) {
        LoginResult loginResult = oAuthService.loginWithGoogle(code, request);
        oAuthService.redirectAfterOAuthLogin(loginResult, AUTH_GOOGLE, response);
    }

    @PostMapping("/accounts")
    public ResponseEntity<?> join(@RequestBody @Valid JoinReq request) {
        userService.join(request);
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.CREATED, null));
    }

    @PostMapping("/accounts/social")
    public ResponseEntity<?> socialJoin(@RequestBody @Valid SocialJoinReq request) {
        userService.socialJoin(request);
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.CREATED, null));
    }

    @PostMapping("/accounts/check/email")
    public ResponseEntity<?> checkEmailAndSendCode(@RequestBody @Valid EmailReq request) {
        CheckAccountExistRes response = userService.checkAccountExist(request.email());
        if (response.isValid()) userService.sendCodeByEmail(request.email(), CODE_TYPE_JOIN);
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, response));
    }

    @PostMapping("/accounts/verify/code")
    public ResponseEntity<?> verifyCode(@RequestBody @Valid VerifyCodeReq request, @RequestParam String codeType) {
        VerifyEmailCodeRes response = userService.verifyCode(request, codeType);
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, response));
    }

    @PostMapping("/accounts/resend/code")
    public ResponseEntity<?> resendCode(@RequestBody @Valid EmailReq request, @RequestParam String codeType) {
        userService.sendCodeByEmail(request.email(), codeType);
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, null));
    }

    @PostMapping("/accounts/check/nick")
    public ResponseEntity<?> checkNickname(@RequestBody @Valid CheckNickReq request) {
        CheckNickNameRes response = userService.checkNickName(request);
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, response));
    }

    @PostMapping("/accounts/withdraw/code")
    public ResponseEntity<?> sendCodeForWithdraw(@RequestBody @Valid EmailReq request) {
        CheckAccountExistRes response = userService.checkAccountExist(request.email());
        if (response.isValid()) userService.sendCodeByEmail(request.email(), CODE_TYPE_WITHDRAW);
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, response));
    }

    @PostMapping("/accounts/recovery/code")
    public ResponseEntity<?> sendCodeForRecovery(@RequestBody @Valid EmailReq request) {
        CheckLocalAccountExistRes response = userService.checkLocalAccountExist(request);
        if (response.isValid()) userService.sendCodeByEmail(request.email(), CODE_TYPE_RECOVERY);
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, response));
    }

    @PostMapping("/accounts/recovery/reset")
    public ResponseEntity<?> resetPassword(@RequestBody @Valid ResetPasswordReq request) {
        userService.resetPassword(request);
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, null));
    }

    @PostMapping("/accounts/password/verify")
    public ResponseEntity<?> verifyPassword(@RequestBody @Valid CurPasswordReq request, @AuthenticationPrincipal CustomUserDetails userDetails) {
        VerifyPasswordRes response = userService.verifyPassword(request, userDetails.getUserId());
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, response));
    }

    @PatchMapping("/accounts/password")
    public ResponseEntity<?> updatePassword(@RequestBody @Valid UpdatePasswordReq request, @AuthenticationPrincipal CustomUserDetails userDetails) {
        userService.updatePassword(request, userDetails.getUserId());
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, null));
    }

    @GetMapping("/accounts/profile")
    public ResponseEntity<?> findProfile(@AuthenticationPrincipal CustomUserDetails userDetails) {
        ProfileRes response = userService.findProfile(userDetails.getUserId());
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, response));
    }

    @PatchMapping("/accounts/profile")
    public ResponseEntity<?> updateProfile(@RequestBody @Valid UpdateProfileReq request, @AuthenticationPrincipal CustomUserDetails userDetails) {
        userService.updateProfile(request, userDetails.getUserId());
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, null));
    }

    @PatchMapping("/auth/access")
    public ResponseEntity<?> updateAccessToken(@CookieValue String refreshToken) {
        String accessToken = userService.updateAccessToken(refreshToken);
        return ResponseEntity.ok()
                .body(ApiUtils.success(HttpStatus.OK, new AccessTokenRes(accessToken)));
    }

    @DeleteMapping("/accounts/withdraw")
    public ResponseEntity<?> withdrawMember(@AuthenticationPrincipal CustomUserDetails userDetails) {
        userService.withdrawMember(userDetails.user().getId());
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, null));
    }

    @PostMapping("/validate/accessToken")
    public ResponseEntity<?> validateAccessToken(@CookieValue String accessToken) {
        ValidateAccessTokenRes response = userService.validateAccessToken(accessToken);
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, response));
    }

    @GetMapping("/communityStats")
    public ResponseEntity<?> findCommunityStats(@AuthenticationPrincipal CustomUserDetails userDetails) {
        FindCommunityRecordRes response = userService.findCommunityStats(userDetails.user().getId());
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, response));
    }
}