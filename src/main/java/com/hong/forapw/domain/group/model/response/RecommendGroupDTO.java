package com.hong.forapw.domain.group.model.response;

import com.hong.forapw.domain.group.entity.Group;
import com.hong.forapw.domain.region.constant.District;
import com.hong.forapw.domain.region.constant.Province;

public record RecommendGroupDTO(
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

    public RecommendGroupDTO(Group group, Long likeNum, boolean isLike) {
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
                isLike,
                group.isShelterOwns(),
                group.getShelterName());
    }
}