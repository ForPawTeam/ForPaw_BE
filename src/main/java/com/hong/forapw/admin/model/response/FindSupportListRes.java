package com.hong.forapw.admin.model.response;

import com.hong.forapw.domain.inquiry.constant.InquiryStatus;
import com.hong.forapw.domain.inquiry.constant.InquiryType;
import com.hong.forapw.domain.inquiry.entity.Inquiry;

import java.time.LocalDateTime;
import java.util.List;

public record FindSupportListRes(List<InquiryDTO> inquiries, int totalPages) {

    public record InquiryDTO(
            Long id,
            LocalDateTime date,
            String questionerNick,
            InquiryType type,
            String title,
            InquiryStatus status
    ) {

        public static List<InquiryDTO> fromEntities(List<Inquiry> inquiries) {
            return inquiries.stream()
                    .map(inquiry -> new FindSupportListRes.InquiryDTO(
                            inquiry.getId(),
                            inquiry.getCreatedDate(),
                            inquiry.getQuestioner().getNickname(),
                            inquiry.getType(),
                            inquiry.getTitle(),
                            inquiry.getStatus())
                    )
                    .toList();
        }
    }
}