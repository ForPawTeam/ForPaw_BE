package com.hong.forapw.domain.group.model.request;

import com.hong.forapw.domain.group.constant.GroupRole;
import jakarta.validation.constraints.NotNull;

public record UpdateUserRoleReq(
        @NotNull(message = "id를 입력해주세요.")
        Long userId,
        GroupRole role) {
}