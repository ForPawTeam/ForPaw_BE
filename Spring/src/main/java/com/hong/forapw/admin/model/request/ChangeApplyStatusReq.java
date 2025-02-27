package com.hong.forapw.admin.model.request;

import com.hong.forapw.domain.apply.constant.ApplyStatus;
import jakarta.validation.constraints.NotNull;

public record ChangeApplyStatusReq(
        @NotNull(message = "변경하려는 지원서의 ID를 입력해주세요.")
        Long id,
        @NotNull(message = "변경하려는 상태를 입력해주세요.")
        ApplyStatus status) {
}
