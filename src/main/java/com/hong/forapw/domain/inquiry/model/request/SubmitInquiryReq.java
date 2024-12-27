package com.hong.forapw.domain.inquiry.model.request;

import com.hong.forapw.domain.inquiry.constant.InquiryStatus;
import com.hong.forapw.domain.inquiry.constant.InquiryType;
import com.hong.forapw.domain.inquiry.entity.Inquiry;
import com.hong.forapw.domain.user.entity.User;
import jakarta.validation.constraints.NotBlank;

public record SubmitInquiryReq(
        @NotBlank(message = "제목을 입력해주세요.")
        String title,
        @NotBlank(message = "문의 내용을 입력해주세요.")
        String description,
        @NotBlank(message = "답변을 받을 이메일 입력해주세요.")
        String contactMail,
        String imageURL,
        InquiryType inquiryType
) {
    public Inquiry toEntity(InquiryStatus status, User user) {
        return Inquiry.builder()
                .questioner(user)
                .title(title)
                .description(description)
                .contactMail(contactMail)
                .status(status)
                .type(inquiryType)
                .imageURL(imageURL)
                .build();
    }
}
