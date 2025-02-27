package com.hong.forapw.domain.group.model.response;

import com.hong.forapw.domain.group.constant.GroupRole;
import com.hong.forapw.domain.group.entity.GroupUser;

import java.time.LocalDateTime;

public record MemberDTO(
        Long id,
        String name,
        GroupRole role,
        String profileURL,
        LocalDateTime joinDate
) {

    public MemberDTO(GroupUser groupUser) {
        this(
                groupUser.getUserId(),
                groupUser.getUserNickname(),
                groupUser.getGroupRole(),
                groupUser.getUserProfileURL(),
                groupUser.getCreatedDate()
        );
    }
}
