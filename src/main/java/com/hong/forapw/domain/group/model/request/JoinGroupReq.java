package com.hong.forapw.domain.group.model.request;

import jakarta.validation.constraints.NotBlank;

public record JoinGroupReq(
        @NotBlank(message = "가입 인사말을 입력해주세요.")
        String greeting) {
}