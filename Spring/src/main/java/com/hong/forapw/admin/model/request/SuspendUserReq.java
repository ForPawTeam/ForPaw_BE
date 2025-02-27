package com.hong.forapw.admin.model.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SuspendUserReq(
        @NotNull(message = "정지하려는 유저의 ID를 입력해주세요.")
        Long userId,
        @NotNull(message = "정지 기간을 입력해주세요.")
        Long suspensionDays,
        @NotBlank(message = "정지 사유를 입력해주세요.")
        String suspensionReason) {
}