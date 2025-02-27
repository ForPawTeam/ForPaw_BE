package com.hong.forapw.domain.group.model.response;

import com.hong.forapw.domain.group.entity.Group;
import com.hong.forapw.domain.region.constant.District;
import com.hong.forapw.domain.region.constant.Province;

public record NewGroupDTO(
        Long id,
        String name,
        String category,
        Province province,
        District district,
        String profileURL
) {

    public NewGroupDTO(Group group) {
        this(
                group.getId(),
                group.getName(),
                group.getCategory(),
                group.getProvince(),
                group.getDistrict(),
                group.getProfileURL()
        );
    }
}