package com.hong.forapw.domain.inquiry;

import com.hong.forapw.domain.inquiry.model.InquriyRequest;
import com.hong.forapw.domain.inquiry.entity.Inquiry;
import com.hong.forapw.domain.inquiry.constant.InquiryStatus;
import com.hong.forapw.domain.user.entity.User;


public class InquiryMapper {

    private InquiryMapper() {
    }

    public static Inquiry buildInquiry(InquriyRequest.SubmitInquiry requestDTO, InquiryStatus status, User user) {
        return Inquiry.builder()
                .questioner(user)
                .title(requestDTO.title())
                .description(requestDTO.description())
                .contactMail(requestDTO.contactMail())
                .status(status)
                .type(requestDTO.inquiryType())
                .imageURL(requestDTO.imageURL())
                .build();
    }
}
