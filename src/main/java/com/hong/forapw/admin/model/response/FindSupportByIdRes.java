package com.hong.forapw.admin.model.response;

import com.hong.forapw.domain.inquiry.entity.Inquiry;

public record FindSupportByIdRes(
        Long id,
        String questionerNick,
        String title,
        String description
) {
    public FindSupportByIdRes(Inquiry inquiry) {
        this(
                inquiry.getId(),
                inquiry.getQuestioner().getNickname(),
                inquiry.getTitle(),
                inquiry.getDescription()
        );
    }
}