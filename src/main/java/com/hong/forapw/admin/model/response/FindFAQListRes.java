package com.hong.forapw.admin.model.response;

import com.hong.forapw.domain.faq.FaqType;

import java.util.List;

public record FindFAQListRes(List<FaqDTO> faqs) {

    public record FaqDTO(
            String question,
            String answer,
            FaqType type,
            boolean isTop) {
    }
}