package com.hong.forapw.domain.inquiry.model;

import com.hong.forapw.domain.inquiry.constant.InquiryStatus;
import com.hong.forapw.domain.inquiry.constant.InquiryType;

import java.time.LocalDateTime;

public record InquiryRes(
        Long id,
        String title,
        String description,
        InquiryStatus status,
        String imageURL,
        InquiryType inquiryType,
        LocalDateTime createdDate,
        AnswerDTO answer) {

    public record AnswerDTO(
            String content,
            String answeredBy) {
    }
}