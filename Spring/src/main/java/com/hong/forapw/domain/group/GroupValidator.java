package com.hong.forapw.domain.group;

import com.hong.forapw.common.exceptions.CustomException;
import com.hong.forapw.common.exceptions.ExceptionCode;
import com.hong.forapw.domain.group.constant.GroupRole;
import com.hong.forapw.domain.group.entity.GroupUser;
import com.hong.forapw.domain.group.repository.GroupRepository;
import com.hong.forapw.domain.group.repository.GroupUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GroupValidator {

    private final GroupRepository groupRepository;
    private final GroupUserRepository groupUserRepository;

    public void validateGroupNameNotDuplicate(String groupName) {
        if (groupRepository.existsByName(groupName)) {
            throw new CustomException(ExceptionCode.DUPLICATE_GROUP_NAME);
        }
    }

    public void validateGroupNameNotDuplicateExcludingId(Long groupId, String groupName) {
        if (groupRepository.existsByNameExcludingId(groupName, groupId)) {
            throw new CustomException(ExceptionCode.DUPLICATE_GROUP_NAME);
        }
    }

    public void validateGroupExists(Long groupId) {
        if (!groupRepository.existsById(groupId)) {
            throw new CustomException(ExceptionCode.GROUP_NOT_FOUND);
        }
    }

    public void validateGroupMembership(Long groupId, Long userId) {
        GroupUser groupUser = groupUserRepository.findByGroupIdAndUserId(groupId, userId)
                .orElseThrow(() -> new CustomException(ExceptionCode.NOT_GROUP_MEMBER));

        if (groupUser.getGroupRole() == GroupRole.TEMP) {
            throw new CustomException(ExceptionCode.NOT_GROUP_MEMBER);
        }
    }

    public void validateNotAlreadyGroupMember(Long groupId, Long userId) {
        if (groupUserRepository.existsByGroupIdAndUserId(groupId, userId)) {
            throw new CustomException(ExceptionCode.ALREADY_IN_GROUP);
        }
    }

    public void validateGroupAdminAuthorization(Long adminId, Long groupId) {
        GroupUser groupUser = groupUserRepository.findByGroupIdAndUserId(groupId, adminId)
                .orElseThrow(() -> new CustomException(ExceptionCode.NOT_GROUP_MEMBER));

        if (groupUser.getGroupRole() != GroupRole.ADMIN && groupUser.getGroupRole() != GroupRole.CREATOR) {
            throw new CustomException(ExceptionCode.NOT_GROUP_ADMIN);
        }
    }

    public void validateGroupCreatorAuthorization(Long groupId, Long userId) {
        GroupUser groupUser = groupUserRepository.findByGroupIdAndUserId(groupId, userId)
                .orElseThrow(() -> new CustomException(ExceptionCode.NOT_GROUP_MEMBER));

        if(groupUser.isNotCreator()){
            throw new CustomException(ExceptionCode.NOT_GROUP_CREATOR);
        }
    }

    public void validateRoleUpdateConstraints(GroupRole newRole, Long userIdToUpdate, Long creatorId) {
        if (userIdToUpdate.equals(creatorId)) { // 그룹장은 자신의 역할을 변경할 수 없음
            throw new CustomException(ExceptionCode.CREATOR_ROLE_UPDATE_FORBIDDEN);
        }

        if (newRole == GroupRole.CREATOR) { // 그룹장으로의 변경은 불가능
            throw new CustomException(ExceptionCode.ROLE_CANT_UPDATE);
        }
    }
}