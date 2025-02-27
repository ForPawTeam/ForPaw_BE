package com.hong.forapw.domain.group.model.request;

import com.hong.forapw.domain.region.constant.District;
import com.hong.forapw.domain.region.constant.Province;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpdateGroupReq(
        @NotBlank(message = "그룹의 이름을 입력해주세요.")
        String name,
        @NotNull(message = "활동 지역을 입력해주세요.")
        Province province,
        @NotNull(message = "활동 지역을 입력해주세요.")
        District district,
        String subDistrict,
        @NotBlank(message = "그룹의 설명을 입력해주세요.")
        String description,
        String category,
        @NotBlank
        String profileURL,
        Long maxNum) {
}