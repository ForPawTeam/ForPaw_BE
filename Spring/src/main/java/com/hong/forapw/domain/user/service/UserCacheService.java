package com.hong.forapw.domain.user.service;

import com.hong.forapw.common.exceptions.CustomException;
import com.hong.forapw.common.exceptions.ExceptionCode;
import com.hong.forapw.domain.user.entity.User;
import com.hong.forapw.domain.user.model.LoginResult;
import com.hong.forapw.integration.redis.RedisService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import static com.hong.forapw.common.constants.GlobalConstants.REFRESH_TOKEN_KEY;

@Service
@RequiredArgsConstructor
public class UserCacheService {

    private final RedisService redisService;

    @Value("${jwt.refresh-exp-milli}")
    public Long refreshExpMilli;

    private static final long VERIFICATION_CODE_EXPIRATION_MS = 175 * 1000L;
    private static final String EMAIL_CODE_KEY_PREFIX = "code:";
    private static final String CODE_TO_EMAIL_KEY_PREFIX = "codeToEmail";
    private static final long LOGIN_FAIL_CURRENT_EXPIRATION_MS = 300_000L; // 5분
    private static final String MAX_LOGIN_ATTEMPTS_BEFORE_LOCK = "loginFail";
    private static final String MAX_DAILY_LOGIN_FAILURES = "loginFailDaily";
    private static final long LOGIN_FAIL_DAILY_EXPIRATION_MS = 86400000L; // 24시간

    public void storeVerificationCode(String email, String codeType, String verificationCode) {
        redisService.storeValue(getCodeTypeKey(codeType), email, verificationCode, VERIFICATION_CODE_EXPIRATION_MS);
    }

    public void storeCodeToEmail(String verificationCode, String email) {
        redisService.storeValue(CODE_TO_EMAIL_KEY_PREFIX, verificationCode, email, LOGIN_FAIL_CURRENT_EXPIRATION_MS);
    }

    public void storeRefreshTokens(Long userId, LoginResult loginResult) {
        redisService.storeValue(REFRESH_TOKEN_KEY, userId.toString(), loginResult.refreshToken(), refreshExpMilli);
    }

    public long incrementDailyLoginFailures(User user) {
        long dailyFailures = redisService.getValueInLong(MAX_DAILY_LOGIN_FAILURES, user.getId().toString());
        dailyFailures++;
        redisService.storeValue(MAX_DAILY_LOGIN_FAILURES, user.getId().toString(), Long.toString(dailyFailures), LOGIN_FAIL_DAILY_EXPIRATION_MS);
        return dailyFailures;
    }

    public long incrementCurrentLoginFailures(Long userId) {
        long currentFailures = redisService.getValueInLong(MAX_LOGIN_ATTEMPTS_BEFORE_LOCK, userId.toString());
        currentFailures++;
        redisService.storeValue(MAX_LOGIN_ATTEMPTS_BEFORE_LOCK, userId.toString(), Long.toString(currentFailures), LOGIN_FAIL_CURRENT_EXPIRATION_MS);
        return currentFailures;
    }

    public void deleteUserTokens(Long userId) {
        redisService.removeValue(REFRESH_TOKEN_KEY, userId.toString());
    }

    public void deleteCodeToEmail(String verificationCode) {
        redisService.removeValue(CODE_TO_EMAIL_KEY_PREFIX, verificationCode);
    }

    public void validateRefreshToken(String refreshToken, Long userId) {
        if (redisService.doesValueMismatch(REFRESH_TOKEN_KEY, userId.toString(), refreshToken)) {
            throw new CustomException(ExceptionCode.INVALID_REFRESH_TOKEN);
        }
    }

    public void validateEmailCodeNotSent(String email, String codeType) {
        if (redisService.isValueStored(getCodeTypeKey(codeType), email)) {
            throw new CustomException(ExceptionCode.EMAIL_ALREADY_SENT);
        }
    }

    public boolean isCodeMismatch(String email, String code, String codeType) {
        return redisService.doesValueMismatch(getCodeTypeKey(codeType), email, code);
    }

    public long getDailyLoginFailures(Long userId) {
        return redisService.getValueInLong(MAX_DAILY_LOGIN_FAILURES, userId.toString());
    }

    public long getCurrentLoginFailures(Long userId) {
        return redisService.getValueInLong(MAX_LOGIN_ATTEMPTS_BEFORE_LOCK, userId.toString());
    }

    public String getEmailByVerificationCode(String verificationCode) {
        return redisService.getValueInString(CODE_TO_EMAIL_KEY_PREFIX, verificationCode);
    }

    private String getCodeTypeKey(String codeType) {
        return EMAIL_CODE_KEY_PREFIX + codeType;
    }
}
