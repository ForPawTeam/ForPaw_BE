package com.hong.forapw.domain.group.model.response;

import com.hong.forapw.domain.group.constant.GroupRole;
import com.hong.forapw.domain.group.entity.GroupUser;

import java.time.LocalDateTime;
import java.util.List;

public record FindGroupMemberListRes(
        Long participantCnt,
        Long maxNum,
        List<MemberDetailDTO> members
) {

    public record MemberDetailDTO(
            Long id,
            String nickName,
            String profileURL,
            LocalDateTime joinDate,
            GroupRole role
    ) {
        public MemberDetailDTO(GroupUser groupUser) {
            this(
                    groupUser.getUserId(),
                    groupUser.getUserNickname(),
                    groupUser.getUserProfileURL(),
                    groupUser.getCreatedDate(),
                    groupUser.getGroupRole()
            );
        }
    }
}