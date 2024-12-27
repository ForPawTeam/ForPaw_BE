package com.hong.forapw.domain.user.model.request;

import com.hong.forapw.domain.region.constant.District;
import com.hong.forapw.domain.region.constant.Province;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpdateProfileReq(
        @NotBlank(message = "닉네임을 입력해주세요.")
        String nickName,

        @NotNull(message = "활동 지역을 입력해주세요.")
        Province province,

        @NotNull(message = "활동 지역을 입력해주세요.")
        District district,

        String subDistrict,

        @NotBlank(message = "프로필 URL을 입력해주세요.")
        String profileURL) {
}