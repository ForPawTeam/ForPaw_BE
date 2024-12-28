package com.hong.forapw.admin.model.response;

import com.hong.forapw.domain.inquiry.constant.InquiryStatus;
import com.hong.forapw.domain.inquiry.constant.InquiryType;

import java.time.LocalDateTime;
import java.util.List;

public record FindSupportListRes(List<InquiryDTO> inquiries, int totalPages) {

    public record InquiryDTO(
            Long id,
            LocalDateTime date,
            String questionerNick,
            InquiryType type,
            String title,
            InquiryStatus status) {
    }
}