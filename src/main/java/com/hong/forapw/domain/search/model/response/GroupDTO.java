package com.hong.forapw.domain.search.model.response;

import com.hong.forapw.domain.group.entity.Group;
import com.hong.forapw.domain.region.constant.District;
import com.hong.forapw.domain.region.constant.Province;

import java.util.Map;

public record GroupDTO(
        Long id,
        String name,
        String description,
        String category,
        Province province,
        District district,
        String profileURL,
        Long participantNum,
        Long meetingNum
) {
    public static GroupDTO fromEntity(Group group, Map<Long, Long> meetingCountsMap) {
        return new GroupDTO(
                group.getId(),
                group.getName(),
                group.getDescription(),
                group.getCategory(),
                group.getProvince(),
                group.getDistrict(),
                group.getProfileURL(),
                group.getParticipantNum(),
                meetingCountsMap.getOrDefault(group.getId(), 0L)
        );
    }
}