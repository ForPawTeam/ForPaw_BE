package com.hong.forapw.domain.group.model.response;

import com.hong.forapw.domain.group.entity.Group;
import com.hong.forapw.domain.region.constant.District;
import com.hong.forapw.domain.region.constant.Province;

import java.util.List;
import java.util.Map;

public record MyGroupDTO(
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

    public MyGroupDTO(Group group, Map<Long, Long> likeCountMap, List<Long> likedGroupIds) {
        this(
                group.getId(),
                group.getName(),
                group.getDescription(),
                group.getParticipantNum(),
                group.getCategory(),
                group.getProvince(),
                group.getDistrict(),
                group.getProfileURL(),
                likeCountMap.getOrDefault(group.getId(), 0L),
                likedGroupIds.contains(group.getId()),
                group.isShelterOwns(),
                group.getShelterName()
        );
    }

    public static List<MyGroupDTO> fromEntities(List<Group> groups, Map<Long, Long> likeCountMap, List<Long> likedGroupIds) {
        return groups.stream()
                .map(group -> new MyGroupDTO(group, likeCountMap, likedGroupIds))
                .toList();
    }
}