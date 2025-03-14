package com.hong.forapw.domain.group.model.request;

import jakarta.validation.constraints.NotNull;

public record ApproveJoinReq(
        @NotNull(message = "id를 입력해주세요.")
        Long applicantId) {
}