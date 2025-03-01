package com.hong.forapw.domain.user.service;

import com.hong.forapw.domain.user.UserValidator;
import com.hong.forapw.domain.post.model.query.PostTypeCount;
import com.hong.forapw.common.exceptions.CustomException;
import com.hong.forapw.common.exceptions.ExceptionCode;
import com.hong.forapw.domain.post.constant.PostType;
import com.hong.forapw.domain.user.entity.User;
import com.hong.forapw.domain.user.entity.UserStatus;
import com.hong.forapw.domain.alarm.repository.AlarmRepository;
import com.hong.forapw.domain.animal.repository.FavoriteAnimalRepository;
import com.hong.forapw.admin.repository.LoginAttemptRepository;
import com.hong.forapw.admin.repository.VisitRepository;
import com.hong.forapw.domain.chat.repository.ChatUserRepository;
import com.hong.forapw.domain.group.repository.FavoriteGroupRepository;
import com.hong.forapw.domain.group.repository.GroupUserRepository;
import com.hong.forapw.domain.meeting.repository.MeetingUserRepository;
import com.hong.forapw.domain.post.repository.CommentLikeRepository;
import com.hong.forapw.domain.post.repository.CommentRepository;
import com.hong.forapw.domain.post.repository.PostLikeRepository;
import com.hong.forapw.domain.post.repository.PostRepository;
import com.hong.forapw.domain.user.model.request.*;
import com.hong.forapw.domain.user.model.response.*;
import com.hong.forapw.domain.user.repository.UserRepository;
import com.hong.forapw.domain.user.repository.UserStatusRepository;
import com.hong.forapw.integration.email.model.EmailVerificationTemplate;
import com.hong.forapw.integration.email.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.hong.forapw.security.jwt.JwtUtils;

import java.util.*;
import java.util.stream.Collectors;

import static com.hong.forapw.common.constants.GlobalConstants.*;
import static com.hong.forapw.common.utils.GeneratorUtils.generatePassword;
import static com.hong.forapw.common.utils.GeneratorUtils.generateVerificationCode;
import static com.hong.forapw.integration.email.EmailTemplate.ACCOUNT_SUSPENSION;
import static com.hong.forapw.integration.email.EmailTemplate.VERIFICATION_CODE;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class UserService {

    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final AlarmRepository alarmRepository;
    private final GroupUserRepository groupUserRepository;
    private final MeetingUserRepository meetingUserRepository;
    private final ChatUserRepository chatUserRepository;
    private final PostLikeRepository postLikeRepository;
    private final CommentRepository commentRepository;
    private final CommentLikeRepository commentLikeRepository;
    private final PostRepository postRepository;
    private final LoginAttemptRepository loginAttemptRepository;
    private final UserStatusRepository userStatusRepository;
    private final VisitRepository visitRepository;
    private final FavoriteAnimalRepository favoriteAnimalRepository;
    private final FavoriteGroupRepository favoriteGroupRepository;
    private final EmailService emailService;
    private final UserCacheService userCacheService;
    private final JwtUtils jwtUtils;
    private final UserValidator validator;

    private static final String CODE_TYPE_RECOVERY = "recovery";

    @Transactional
    public void join(JoinReq request) {
        validator.validateConfirmPasswordMatch(request.password(), request.passwordConfirm());
        validator.validateEmailNotRegistered(request.email());
        validator.validateNicknameUniqueness(request.nickName());

        User user = request.toEntity(passwordEncoder.encode(request.password()));
        userRepository.save(user);

        setUserStatus(user);
    }

    @Transactional
    public void socialJoin(SocialJoinReq request) {
        validator.validateEmailNotRegistered(request.email());
        validator.validateNicknameUniqueness(request.nickName());

        User user = request.toEntity(passwordEncoder.encode(generatePassword()));
        userRepository.save(user);

        setUserStatus(user);
    }

    @Async
    public void sendCodeByEmail(String email, String codeType) {
        userCacheService.validateEmailCodeNotSent(email, codeType);

        String verificationCode = generateVerificationCode();
        userCacheService.storeVerificationCode(email, codeType, verificationCode);

        EmailVerificationTemplate templateModel = new EmailVerificationTemplate(verificationCode);
        emailService.sendMail(email, VERIFICATION_CODE.getSubject(), MAIL_TEMPLATE_FOR_CODE, templateModel);
    }

    public VerifyEmailCodeRes verifyCode(VerifyCodeReq request, String codeType) {
        if (userCacheService.isCodeMismatch(request.email(), request.code(), codeType))
            return new VerifyEmailCodeRes(false);

        if (CODE_TYPE_RECOVERY.equals(codeType))
            userCacheService.storeCodeToEmail(request.code(), request.email());

        return new VerifyEmailCodeRes(true);
    }

    public CheckNickNameRes checkNickName(CheckNickReq request) {
        boolean isDuplicate = userRepository.existsByNicknameWithRemoved(request.nickName());
        return new CheckNickNameRes(isDuplicate);
    }

    public CheckLocalAccountExistRes checkLocalAccountExist(EmailReq requestDTO) {
        return userRepository.findByEmail(requestDTO.email())
                .map(user -> new CheckLocalAccountExistRes(true, user.isLocalJoined()))
                .orElse(new CheckLocalAccountExistRes(false, false));
    }

    public CheckAccountExistRes checkAccountExist(String email) {
        boolean isValid = !userRepository.existsByEmail(email);
        return new CheckAccountExistRes(isValid);
    }

    @Transactional
    public void resetPassword(ResetPasswordReq request) {
        String email = userCacheService.getEmailByVerificationCode(request.code());
        if (email == null) {
            throw new CustomException(ExceptionCode.BAD_APPROACH);
        }

        userCacheService.deleteCodeToEmail(request.code());
        updateNewPassword(email, request.newPassword());
    }

    public VerifyPasswordRes verifyPassword(CurPasswordReq request, Long userId) {
        User user = userRepository.findNonWithdrawnById(userId)
                .orElseThrow(() -> new CustomException(ExceptionCode.USER_NOT_FOUND));

        if (isPasswordUnmatched(user, request.password())) {
            return new VerifyPasswordRes(false);
        }

        return new VerifyPasswordRes(true);
    }

    @Transactional
    public void updatePassword(UpdatePasswordReq request, Long userId) {
        User user = userRepository.findNonWithdrawnById(userId)
                .orElseThrow(() -> new CustomException(ExceptionCode.USER_NOT_FOUND));

        validator.validatePasswordMatch(user, request.curPassword());
        validator.validateConfirmPasswordMatch(request.curPassword(), request.newPasswordConfirm());

        user.updatePassword(passwordEncoder.encode(request.newPassword()));
    }

    public ProfileRes findProfile(Long userId) {
        User user = userRepository.findNonWithdrawnById(userId)
                .orElseThrow(() -> new CustomException(ExceptionCode.USER_NOT_FOUND));

        return new ProfileRes(user);
    }

    @Transactional
    public void updateProfile(UpdateProfileReq request, Long userId) {
        User user = userRepository.findNonWithdrawnById(userId)
                .orElseThrow(() -> new CustomException(ExceptionCode.USER_NOT_FOUND));

        validator.validateNickname(user, request.nickName());
        user.updateProfile(request.nickName(), request.province(), request.district(), request.subDistrict(), request.profileURL());
    }

    public String updateAccessToken(String refreshToken) {
        User user = jwtUtils.getUserFromToken(refreshToken)
                .orElseThrow(() -> new CustomException(ExceptionCode.INVALID_TOKEN));

        userCacheService.validateRefreshToken(refreshToken, user.getId());

        return createAccessToken(user);
    }

    // 게시글, 댓글, 좋아요은 남겨둔다. (정책에 따라 변경 가능)
    @Transactional
    public void withdrawMember(Long userId) {
        User user = userRepository.findByIdWithRemoved(userId)
                .orElseThrow(() -> new CustomException(ExceptionCode.USER_NOT_FOUND));

        validator.validateUserNotExited(user);
        validator.validateUserIsNotGroupCreator(user);

        deleteUserRelatedData(userId);
        deleteUserAssociations(userId);
        userCacheService.deleteUserTokens(userId);
        user.deactivateUser();

        userRepository.markAsRemovedById(userId);
    }

    public ValidateAccessTokenRes validateAccessToken(String accessToken) {
        Long userId = jwtUtils.getUserIdFromToken(accessToken)
                .orElseThrow(() -> new CustomException(ExceptionCode.INVALID_TOKEN));

        String profile = userRepository.findProfileById(userId).orElse(null);
        return new ValidateAccessTokenRes(profile);
    }

    public FindCommunityRecordRes findCommunityStats(Long userId) {
        User user = userRepository.findNonWithdrawnById(userId)
                .orElseThrow(() -> new CustomException(ExceptionCode.USER_NOT_FOUND));

        List<PostTypeCount> postTypeCounts = postRepository.countByUserIdAndType(userId, ALL_POST_TYPES);
        Map<PostType, Long> postCountMap = postTypeCounts.stream()
                .collect(Collectors.toMap(PostTypeCount::postType, PostTypeCount::count));

        Long adoptionNum = postCountMap.getOrDefault(PostType.ADOPTION, 0L);
        Long fosteringNum = postCountMap.getOrDefault(PostType.FOSTERING, 0L);
        Long questionNum = postCountMap.getOrDefault(PostType.QUESTION, 0L);
        Long answerNum = postCountMap.getOrDefault(PostType.ANSWER, 0L);
        Long commentNum = commentRepository.countByUserId(userId);

        return new FindCommunityRecordRes(user, adoptionNum + fosteringNum, commentNum, questionNum, answerNum);
    }

    private void updateNewPassword(String email, String newPassword) {
        User user = userRepository.findByEmail(email).orElseThrow(
                () -> new CustomException(ExceptionCode.EMAIL_NOT_FOUND)
        );

        user.updatePassword(passwordEncoder.encode(newPassword));
    }

    private String createAccessToken(User user) {
        return jwtUtils.generateAccessToken(user);
    }

    private boolean isPasswordUnmatched(User user, String inputPassword) {
        return !passwordEncoder.matches(inputPassword, user.getPassword());
    }

    private void deleteUserRelatedData(Long userId) {
        alarmRepository.deleteByUserId(userId);
        visitRepository.deleteByUserId(userId);
        loginAttemptRepository.deleteByUserId(userId);
    }

    private void deleteUserAssociations(Long userId) {
        postLikeRepository.deleteByUserId(userId);
        commentLikeRepository.deleteByUserId(userId);
        favoriteAnimalRepository.deleteAllByUserId(userId);
        favoriteGroupRepository.deleteByGroupId(userId);
        chatUserRepository.deleteByUserId(userId);
        deleteAllGroupUserData(userId);
        deleteAllMeetingUserData(userId);
    }

    private void deleteAllMeetingUserData(Long userId) {
        meetingUserRepository.findByUserIdWithMeeting(userId).forEach(
                meetingUser -> {
                    meetingUser.getMeeting().decrementParticipantCount();
                    meetingUserRepository.delete(meetingUser);
                });
    }

    private void deleteAllGroupUserData(Long userId) {
        groupUserRepository.findByUserIdWithGroup(userId).forEach(
                groupUser -> {
                    groupUser.getGroup().decrementParticipantNum();
                    groupUserRepository.delete(groupUser);
                });
    }

    private void setUserStatus(User user) {
        UserStatus status = UserStatus.builder()
                .user(user)
                .isActive(true)
                .build();

        userStatusRepository.save(status);
        user.updateStatus(status);
    }
}