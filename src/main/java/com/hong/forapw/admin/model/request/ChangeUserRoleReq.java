package com.hong.forapw.admin.model.request;

import com.hong.forapw.domain.user.constant.UserRole;
import jakarta.validation.constraints.NotNull;

public record ChangeUserRoleReq(
        @NotNull(message = "변경하려는 유저의 ID를 입력해주세요.")
        Long userId,
        @NotNull(message = "변경하려는 역할을 입력해주세요.")
        UserRole role) {
}