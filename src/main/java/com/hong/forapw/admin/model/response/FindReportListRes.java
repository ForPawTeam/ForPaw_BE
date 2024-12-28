package com.hong.forapw.admin.model.response;

import com.hong.forapw.admin.constant.ContentType;
import com.hong.forapw.admin.constant.ReportStatus;
import com.hong.forapw.admin.constant.ReportType;

import java.time.LocalDateTime;
import java.util.List;

public record FindReportListRes(List<ReportDTO> reports, int totalPages) {

    public record ReportDTO(
            Long id,
            LocalDateTime reportDate,
            ContentType contentType,
            Long contentId,
            ReportType type,
            String reason,
            String reporterNickName,
            Long offenderId,
            String offenderNickName,
            ReportStatus status) {
    }
}