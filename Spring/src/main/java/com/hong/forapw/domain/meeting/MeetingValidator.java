package com.hong.forapw.domain.meeting;

import com.hong.forapw.common.exceptions.CustomException;
import com.hong.forapw.common.exceptions.ExceptionCode;
import com.hong.forapw.domain.group.constant.GroupRole;
import com.hong.forapw.domain.group.entity.GroupUser;
import com.hong.forapw.domain.group.repository.GroupRepository;
import com.hong.forapw.domain.group.repository.GroupUserRepository;
import com.hong.forapw.domain.meeting.model.request.CreateMeetingReq;
import com.hong.forapw.domain.meeting.repository.MeetingRepository;
import com.hong.forapw.domain.meeting.repository.MeetingUserRepository;
import com.hong.forapw.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.Set;


@Component
@RequiredArgsConstructor
public class MeetingValidator {

    private final MeetingRepository meetingRepository;
    private final MeetingUserRepository meetingUserRepository;
    private final GroupUserRepository groupUserRepository;
    private final GroupRepository groupRepository;

    private static final Set<GroupRole> GROUP_USER_TYPES = EnumSet.of(GroupRole.USER, GroupRole.ADMIN, GroupRole.CREATOR);
    private static final Set<GroupRole> GROUP_ADMIN_TYPES = EnumSet.of(GroupRole.ADMIN, GroupRole.CREATOR);

    public void validateMeetingNameNotDuplicate(CreateMeetingReq request, Long groupId) {
        if (meetingRepository.existsByNameAndGroupId(request.name(), groupId)) {
            throw new CustomException(ExceptionCode.DUPLICATE_GROUP_NAME);
        }
    }

    public void validateMeetingParticipation(Long meetingId, Long userId) {
        if (!meetingUserRepository.existsByMeetingIdAndUserId(meetingId, userId)) {
            throw new CustomException(ExceptionCode.NOT_MEETING_MEMBER);
        }
    }

    public void validateNotAlreadyMeetingParticipant(Long meetingId, Long userId) {
        if (meetingUserRepository.existsByMeetingIdAndUserId(meetingId, userId)) {
            throw new CustomException(ExceptionCode.MEETING_ALREADY_JOINED);
        }
    }

    public void validateMeetingExists(Long meetingId) {
        if (!meetingRepository.existsById(meetingId)) {
            throw new CustomException(ExceptionCode.MEETING_NOT_FOUND);
        }
    }

    public void validateGroupMembership(Long groupId, Long userId) {
        GroupUser groupUser = groupUserRepository.findByGroupIdAndUserId(groupId, userId)
                .orElseThrow(() -> new CustomException(ExceptionCode.NOT_GROUP_MEMBER));

        if (!GROUP_USER_TYPES.contains(groupUser.getGroupRole())) {
            throw new CustomException(ExceptionCode.NOT_GROUP_MEMBER);
        }
    }

    public void validateGroupAdminAuthorization(User user, Long groupId) {
        GroupUser groupUser = groupUserRepository.findByGroupIdAndUserId(groupId, user.getId())
                .orElseThrow(() -> new CustomException(ExceptionCode.NOT_GROUP_MEMBER));

        if (!GROUP_ADMIN_TYPES.contains(groupUser.getGroupRole())) {
            throw new CustomException(ExceptionCode.UNAUTHORIZED_ACCESS);
        }
    }

    public void validateGroupExists(Long groupId) {
        if (!groupRepository.existsById(groupId)) {
            throw new CustomException(ExceptionCode.GROUP_NOT_FOUND);
        }
    }
}
