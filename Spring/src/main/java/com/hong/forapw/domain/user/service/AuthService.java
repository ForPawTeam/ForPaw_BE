package com.hong.forapw.domain.user.service;

import com.hong.forapw.admin.entity.LoginAttempt;
import com.hong.forapw.admin.repository.LoginAttemptRepository;
import com.hong.forapw.common.exceptions.CustomException;
import com.hong.forapw.common.exceptions.ExceptionCode;
import com.hong.forapw.domain.user.UserValidator;
import com.hong.forapw.domain.user.entity.User;
import com.hong.forapw.domain.user.model.LoginResult;
import com.hong.forapw.domain.user.model.request.LoginReq;
import com.hong.forapw.domain.user.repository.UserRepository;
import com.hong.forapw.integration.email.EmailService;
import com.hong.forapw.integration.email.model.BlankTemplate;
import com.hong.forapw.integration.oauth.common.OAuthToken;
import com.hong.forapw.integration.oauth.common.SocialOAuthService;
import com.hong.forapw.integration.oauth.SocialProvider;
import com.hong.forapw.integration.oauth.common.SocialUser;
import com.hong.forapw.security.jwt.JwtUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static com.hong.forapw.common.constants.GlobalConstants.*;
import static com.hong.forapw.domain.user.model.LoginResult.isNotJoined;
import static com.hong.forapw.integration.email.EmailTemplate.ACCOUNT_SUSPENSION;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final LoginAttemptRepository loginAttemptRepository;
    private final UserCacheService userCacheService;
    private final EmailService emailService;
    private final UserValidator validator;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final Map<SocialProvider, SocialOAuthService<? extends OAuthToken, ? extends SocialUser>> socialServices;

    private static final String QUERY_PARAM_EMAIL = "email";
    private static final String QUERY_PARAM_AUTH_PROVIDER = "authProvider";
    private static final String QUERY_PARAM_ACCESS_TOKEN = "accessToken";

    @Value("${social.join.redirect.uri}")
    private String redirectJoinUri;

    @Value("${social.home.redirect.uri}")
    private String redirectHomeUri;

    @Transactional
    public LoginResult login(LoginReq request, HttpServletRequest servletRequest) {
        User user = userRepository.findByEmailWithRemoved(request.email()).orElseThrow(
                () -> new CustomException(ExceptionCode.INVALID_CREDENTIALS));

        validator.validateUserNotExited(user);
        validator.validateUserActive(user);
        validator.validateLoginAttempts(user);

        if (isPasswordUnmatched(user, request.password())) {
            handleLoginFailures(user);
            infoLoginFail(user);
        }

        recordLoginAttempt(user, servletRequest);
        return createToken(user);
    }

    @Transactional
    public LoginResult loginWithSocial(String code, SocialProvider provider, HttpServletRequest request) {
        SocialOAuthService<? extends OAuthToken, ? extends SocialUser> service = socialServices.get(provider);
        if (service == null)
            throw new CustomException(ExceptionCode.INVALID_AUTH_PROVIDER);

        OAuthToken token = service.getToken(code);
        SocialUser userInfo = service.getUserInfo(token.getToken());
        String email = userInfo.getEmail();

        return processSocialLogin(email, request);
    }

    public void redirectAfterOauthLogin(LoginResult loginResult, String authProvider, HttpServletResponse response) {
        try {
            String redirectUri = buildRedirectUri(loginResult, authProvider, response);
            response.sendRedirect(redirectUri);
        } catch (IOException e) {
            log.error("소셜 로그인 증 리다이렉트 에러 발생", e);
            throw new CustomException(ExceptionCode.REDIRECT_ERROR);
        }
    }

    private LoginResult processSocialLogin(String email, HttpServletRequest request) {
        Optional<User> userOP = userRepository.findByEmailWithRemoved(email);
        if (userOP.isEmpty()) {
            return new LoginResult(email, null, null, false);
        }

        User user = userOP.get();
        validator.validateUserNotExited(user);
        validator.validateUserActive(user);
        validator.validateNotLocalSignupAccount(user);

        recordLoginAttempt(user, request);
        return createToken(user);
    }

    private boolean isPasswordUnmatched(User user, String inputPassword) {
        return !passwordEncoder.matches(inputPassword, user.getPassword());
    }

    private void handleLoginFailures(User user) {
        long currentFailures = userCacheService.incrementCurrentLoginFailures(user.getId());

        if (currentFailures >= 3L) {
            long dailyFailures = userCacheService.incrementDailyLoginFailures(user);
            if (dailyFailures == 3L) {
                emailService.sendMail(user.getEmail(), ACCOUNT_SUSPENSION.getSubject(), MAIL_TEMPLATE_FOR_LOCK_ACCOUNT, new BlankTemplate());
            }

            throw new CustomException(ExceptionCode.LOGIN_LIMIT_EXCEEDED);
        }
    }

    private void infoLoginFail(User user) {
        Long currentLoginFailures = userCacheService.getCurrentLoginFailures(user.getId());
        String message = String.format("로그인에 실패했습니다. 이메일 또는 비밀번호를 확인해 주세요. (%d회 실패)", currentLoginFailures);
        throw new CustomException(ExceptionCode.INVALID_CREDENTIALS, message);
    }

    private void recordLoginAttempt(User user, HttpServletRequest request) {
        String clientIp = getClientIP(request);
        String userAgent = request.getHeader(USER_AGENT_HEADER);

        LoginAttempt attempt = LoginAttempt.builder()
                .user(user)
                .clientIp(clientIp)
                .userAgent(userAgent)
                .build();

        loginAttemptRepository.save(attempt);
    }

    private String getClientIP(HttpServletRequest request) {
        return Arrays.stream(IP_HEADER_CANDIDATES)
                .map(request::getHeader)
                .filter(ip -> ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip))
                .findFirst()
                .orElse(request.getRemoteAddr());
    }

    private LoginResult createToken(User user) {
        String accessToken = jwtUtils.generateAccessToken(user);
        String refreshToken = jwtUtils.generateRefreshToken(user);

        LoginResult loginResult = new LoginResult(null, accessToken, refreshToken, true);
        userCacheService.storeRefreshTokens(user.getId(), loginResult);

        return loginResult;
    }

    private String buildRedirectUri(LoginResult result, String authProvider, HttpServletResponse response) {
        Map<String, String> queryParams = new HashMap<>();
        queryParams.put(QUERY_PARAM_AUTH_PROVIDER, authProvider);

        if (isNotJoined(result)) {
            queryParams.put(QUERY_PARAM_EMAIL, result.email());
            return createRedirectUri(redirectJoinUri, queryParams);
        } else {
            response.addHeader(HttpHeaders.SET_COOKIE, jwtUtils.generateRefreshTokenCookie(result.refreshToken()));
            queryParams.put(QUERY_PARAM_ACCESS_TOKEN, result.accessToken());
            return createRedirectUri(redirectHomeUri, queryParams);
        }
    }

    private String createRedirectUri(String baseUri, Map<String, String> queryParams) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(baseUri);
        queryParams.forEach(builder::queryParam); // UriComponentsBuilder의 encode()를 사용해 자동 URL 인코딩을 적용
        return builder.build().toUriString();
    }
}
