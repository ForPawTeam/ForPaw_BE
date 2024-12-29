package com.hong.forapw.domain.user;

import com.hong.forapw.common.exceptions.CustomException;
import com.hong.forapw.common.exceptions.ExceptionCode;
import com.hong.forapw.domain.group.entity.GroupUser;
import com.hong.forapw.domain.group.repository.GroupUserRepository;
import com.hong.forapw.domain.user.entity.User;
import com.hong.forapw.domain.user.repository.UserRepository;
import com.hong.forapw.domain.user.service.UserCacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import static com.hong.forapw.common.constants.GlobalConstants.CURRENT_FAILURE_LIMIT;
import static com.hong.forapw.common.constants.GlobalConstants.DAILY_FAILURE_LIMIT;

@Component
@RequiredArgsConstructor
public class UserValidator {

    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final GroupUserRepository groupUserRepository;
    private final UserCacheService userCacheService;

    public void validateNotLocalSignupAccount(User user) {
        if (user.isLocalJoined()) {
            throw new CustomException(ExceptionCode.LOCAL_SIGNUP_ACCOUNT);
        }
    }

    public void validateConfirmPasswordMatch(String password, String confirmPassword) {
        if (!password.equals(confirmPassword))
            throw new CustomException(ExceptionCode.PASSWORD_MISMATCH);
    }

    public void validateLoginAttempts(User user) {
        long dailyLoginFailures = userCacheService.getDailyLoginFailures(user.getId());
        if (dailyLoginFailures >= DAILY_FAILURE_LIMIT) {
            throw new CustomException(ExceptionCode.ACCOUNT_LOCKED);
        }

        long currentLoginFailures = userCacheService.getCurrentLoginFailures(user.getId());
        if (currentLoginFailures >= CURRENT_FAILURE_LIMIT) {
            throw new CustomException(ExceptionCode.LOGIN_LIMIT_EXCEEDED);
        }
    }

    public void validatePasswordMatch(User user, String inputPassword) {
        if (!passwordEncoder.matches(user.getPassword(), inputPassword))
            throw new CustomException(ExceptionCode.PASSWORD_MISMATCH);
    }

    public void validateEmailNotRegistered(String email) {
        userRepository.findAuthProviderByEmail(email)
                .map(authProvider -> switch (authProvider) {
                    case LOCAL -> new CustomException(ExceptionCode.LOCAL_SIGNUP_ACCOUNT);
                    case GOOGLE, KAKAO -> new CustomException(ExceptionCode.SOCIAL_SIGNUP_ACCOUNT);
                })
                .ifPresent(customException -> {
                    throw customException;
                });
    }

    public void validateNicknameUniqueness(String nickName) {
        if (userRepository.existsByNicknameWithRemoved(nickName))
            throw new CustomException(ExceptionCode.NICKNAME_DUPLICATE);
    }

    public void validateUserNotExited(User user) {
        if (user.isExitMember()) {
            throw new CustomException(ExceptionCode.ACCOUNT_DEACTIVATED);
        }
    }

    public void validateUserIsNotGroupCreator(User user) {
        groupUserRepository.findAllByUser(user)
                .stream()
                .filter(GroupUser::isCreator)
                .findFirst()
                .ifPresent(groupUser -> {
                    throw new CustomException(ExceptionCode.GROUP_CREATOR_CANNOT_LEAVE);
                });
    }

    public void validateUserActive(User user) {
        if (user.isUnActive()) {
            throw new CustomException(ExceptionCode.ACCOUNT_SUSPENDED);
        }
    }

    public void validateNickname(User user, String newNickname) {
        // 현재 닉네임을 유지하고 있으면, 굳이 DB까지 접근해서 검증 필요 X
        if (user.isNickNameUnequal(newNickname) && userRepository.existsByNicknameWithRemoved(newNickname)) {
            throw new CustomException(ExceptionCode.NICKNAME_DUPLICATE);
        }
    }
}
