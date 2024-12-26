package com.hong.forapw.domain.post.model.request;

import com.hong.forapw.admin.constant.ContentType;
import com.hong.forapw.admin.constant.ReportType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SubmitReportReq(
        @NotNull(message = "신고 하려는 컨텐츠의 유형을 선택해주세요.")
        ContentType contentType,
        @NotNull(message = "신고 하려는 컨텐츠의 ID를 입력해주세요.")
        Long contentId,
        @NotNull(message = "신고 유형을 선택해주세요.")
        ReportType reportType,
        @NotBlank(message = "신고 사유를 입력해주세요.")
        String reason) {
}