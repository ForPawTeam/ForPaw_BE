package com.hong.forapw.domain.inquiry.model.response;

import com.hong.forapw.domain.inquiry.constant.InquiryStatus;
import com.hong.forapw.domain.inquiry.constant.InquiryType;
import com.hong.forapw.domain.inquiry.entity.Inquiry;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public record FindInquiryListRes(List<InquiryDTO> inquiries) {

    public record InquiryDTO(
            Long id,
            String title,
            String description,
            InquiryStatus status,
            String imageURL,
            InquiryType inquiryType,
            LocalDateTime createdDate,
            AnswerDTO answer) {

        public static InquiryDTO fromEntity(Inquiry inquiry) {
            AnswerDTO answerDTO = Optional.ofNullable(inquiry.getAnswer())
                    .map(answer -> AnswerDTO.fromEntity(inquiry))
                    .orElse(null);

            return new InquiryDTO(
                    inquiry.getId(),
                    inquiry.getTitle(),
                    inquiry.getDescription(),
                    inquiry.getStatus(),
                    inquiry.getImageURL(),
                    inquiry.getType(),
                    inquiry.getCreatedDate(),
                    answerDTO);
        }
    }

    public record AnswerDTO(
            String content,
            String answeredBy) {

        public static AnswerDTO fromEntity(Inquiry inquiry) {
            return new AnswerDTO(
                    inquiry.getAnswer(),
                    inquiry.getAnswerer().getName());
        }
    }
}
