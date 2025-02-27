package com.hong.forapw.admin.model.response;

import com.hong.forapw.domain.faq.FAQ;
import com.hong.forapw.domain.faq.FaqType;

import java.util.List;

public record FindFAQListRes(List<FaqDTO> faqs) {

    public record FaqDTO(
            String question,
            String answer,
            FaqType type,
            boolean isTop
    ) {

        public static List<FaqDTO> fromEntities(List<FAQ> faqs) {
            return faqs.stream()
                    .map(faq -> new FindFAQListRes.FaqDTO(
                            faq.getQuestion(),
                            faq.getAnswer(),
                            faq.getType(),
                            faq.isTop())
                    )
                    .toList();
        }
    }
}