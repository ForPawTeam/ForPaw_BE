package com.hong.forapw.domain.user.model.request;

import jakarta.validation.constraints.NotBlank;

public record UpdateAccessTokenReq(
        @NotBlank(message = "토큰이 존재하지 않습니다.")
        String refreshToken) {
}
