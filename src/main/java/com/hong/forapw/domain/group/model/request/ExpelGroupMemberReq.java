package com.hong.forapw.domain.group.model.request;

import jakarta.validation.constraints.NotNull;

public record ExpelGroupMemberReq(
        @NotNull(message = "강퇴할 유저 ID를 입력해주세요.")
        Long userId) {
}