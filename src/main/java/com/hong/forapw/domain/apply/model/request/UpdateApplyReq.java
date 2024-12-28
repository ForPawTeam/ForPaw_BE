package com.hong.forapw.domain.apply.model.request;

import jakarta.validation.constraints.NotBlank;

public record UpdateApplyReq(
        @NotBlank(message = "지원자 이름을 입력해주세요.")
        String name,
        @NotBlank(message = "연락처를 입력해주세요.")
        String tel,
        @NotBlank(message = "도로명 주소를 입력해주세요.")
        String roadNameAddress,
        @NotBlank(message = "상세 주소를 입력해주세요.")
        String addressDetail,
        @NotBlank(message = "우편 번호를 입력해주세요.")
        String zipCode) {
}