package com.hong.forapw.domain.report;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum ReportStatus {

    PROCESSING("진행중"),
    PROCESSED("완료됨");

    private String value;
}