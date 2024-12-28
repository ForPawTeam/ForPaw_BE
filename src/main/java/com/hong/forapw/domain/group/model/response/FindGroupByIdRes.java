package com.hong.forapw.domain.group.model.response;

import com.hong.forapw.domain.group.entity.Group;
import com.hong.forapw.domain.region.constant.District;
import com.hong.forapw.domain.region.constant.Province;

public record FindGroupByIdRes(
        String name,
        Province province,
        District district,
        String subDistrict,
        String description,
        String category,
        String profileURL,
        Long maxNum
) {
    public FindGroupByIdRes(Group group) {
        this(
                group.getName(),
                group.getProvince(),
                group.getDistrict(),
                group.getSubDistrict(),
                group.getDescription(),
                group.getCategory(),
                group.getProfileURL(),
                group.getMaxNum()
        );
    }
}