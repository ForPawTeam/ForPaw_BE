package com.hong.forapw.admin.model.request;

import jakarta.validation.constraints.NotNull;

public record ProcessReportReq(
        @NotNull(message = "신고 내역의 ID를 입력해주세요.")
        Long id,
        boolean hasSuspension,
        long suspensionDays,
        boolean hasBlocking) {
}