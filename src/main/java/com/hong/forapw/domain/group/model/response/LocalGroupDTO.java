package com.hong.forapw.domain.group.model.response;

import com.hong.forapw.domain.group.entity.Group;
import com.hong.forapw.domain.region.constant.District;
import com.hong.forapw.domain.region.constant.Province;

public record LocalGroupDTO(
        Long id,
        String name,
        String description,
        Long participationNum,
        String category,
        Province province,
        District district,
        String profileURL,
        Long likeNum,
        boolean isLike,
        boolean isShelterOwns,
        String shelterName
) {

    public LocalGroupDTO(Group group, Long likeNum, boolean isLikedGroup) {
        this(
                group.getId(),
                group.getName(),
                group.getDescription(),
                group.getParticipantNum(),
                group.getCategory(),
                group.getProvince(),
                group.getDistrict(),
                group.getProfileURL(),
                likeNum,
                isLikedGroup,
                group.isShelterOwns(),
                group.getShelterName());
    }
}
