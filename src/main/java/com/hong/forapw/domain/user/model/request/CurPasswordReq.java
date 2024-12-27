package com.hong.forapw.domain.user.model.request;

import jakarta.validation.constraints.NotBlank;

public record CurPasswordReq(
        @NotBlank(message = "현재 비밀번호를 입력해주세요.")
        String password) {
}