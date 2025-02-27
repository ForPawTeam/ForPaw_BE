package com.hong.forapw.admin.model.request;

import jakarta.validation.constraints.NotBlank;

public record AnswerInquiryReq(
        @NotBlank(message = "답변 내용을 입력해주세요.")
        String content) {
}