package com.hong.forapw.admin.model.request;

import com.hong.forapw.domain.faq.FaqType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateFaqReq(
        @NotBlank(message = "질문 내용을 입력해주세요.")
        String question,
        @NotBlank(message = "답변 내용을 입력해주세요.")
        String answer,
        @NotNull(message = "FAQ 타입을 입력해주세요.")
        FaqType type,
        boolean isTop) {
}