package com.hong.forapw.domain.inquiry;

import com.hong.forapw.domain.inquiry.model.request.SubmitInquiryReq;
import com.hong.forapw.domain.inquiry.model.request.UpdateInquiryReq;
import com.hong.forapw.domain.inquiry.model.response.FindInquiryListRes;
import com.hong.forapw.domain.inquiry.model.response.SubmitInquiryRes;
import com.hong.forapw.common.exceptions.CustomException;
import com.hong.forapw.common.exceptions.ExceptionCode;
import com.hong.forapw.domain.inquiry.entity.Inquiry;
import com.hong.forapw.domain.inquiry.constant.InquiryStatus;
import com.hong.forapw.domain.user.entity.User;
import com.hong.forapw.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InquiryService {

    private final InquiryRepository inquiryRepository;
    private final UserRepository userRepository;


    @Transactional
    public SubmitInquiryRes submitInquiry(SubmitInquiryReq request, Long userId) {
        User submitter = userRepository.getReferenceById(userId);
        Inquiry inquiry = request.toEntity(InquiryStatus.PROCESSING, submitter);

        inquiryRepository.save(inquiry);

        return new SubmitInquiryRes(inquiry.getId());
    }

    @Transactional
    public void updateInquiry(UpdateInquiryReq request, Long inquiryId, Long userId) {
        Inquiry inquiry = inquiryRepository.findById(inquiryId).orElseThrow(
                () -> new CustomException(ExceptionCode.INQUIRY_NOT_FOUND)
        );

        checkAuthority(userId, inquiry.getQuestioner());
        inquiry.updateInquiry(request.title(), request.description(), request.contactMail());
    }

    public FindInquiryListRes findInquiries(Long userId) {
        List<Inquiry> inquiries = inquiryRepository.findAllByQuestionerId(userId);
        List<FindInquiryListRes.InquiryDTO> inquiryDTOS = inquiries.stream()
                .map(FindInquiryListRes.InquiryDTO::fromEntity)
                .toList();

        return new FindInquiryListRes(inquiryDTOS);
    }

    private void checkAuthority(Long userId, User writer) {
        if (writer.isNotSameUser(userId)) {
            throw new CustomException(ExceptionCode.UNAUTHORIZED_ACCESS);
        }
    }
}
