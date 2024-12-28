package com.hong.forapw.domain.group.model.response;

import com.hong.forapw.domain.group.entity.GroupUser;
import com.hong.forapw.domain.region.constant.District;
import com.hong.forapw.domain.region.constant.Province;

import java.time.LocalDateTime;
import java.util.List;

public record FindApplicantListRes(List<ApplicantDTO> applicants) {

    public record ApplicantDTO(
            Long id,
            String nickName,
            String greeting,
            String email,
            String profileURL,
            Province province,
            District district,
            LocalDateTime applyDate
    ) {
        public ApplicantDTO(GroupUser groupUser) {
            this(
                    groupUser.getUserId(),
                    groupUser.getUserNickname(),
                    groupUser.getGreeting(),
                    groupUser.getUserEmail(),
                    groupUser.getUserProfileURL(),
                    groupUser.getUserProvince(),
                    groupUser.getUserDistrict(),
                    groupUser.getCreatedDate()
            );
        }

        public static List<ApplicantDTO> toDTOs(List<GroupUser> applicants) {
            return applicants.stream()
                    .map(ApplicantDTO::new)
                    .toList();
        }
    }
}