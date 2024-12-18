package com.hong.forapw.service;

import com.hong.forapw.controller.dto.UserRequest;
import com.hong.forapw.controller.dto.UserResponse;
import com.hong.forapw.core.errors.CustomException;
import com.hong.forapw.core.errors.ExceptionCode;
import com.hong.forapw.core.utils.mapper.InquiryMapper;
import com.hong.forapw.domain.inquiry.Inquiry;
import com.hong.forapw.domain.inquiry.InquiryStatus;
import com.hong.forapw.domain.user.User;
import com.hong.forapw.repository.UserRepository;
import com.hong.forapw.repository.inquiry.InquiryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.hong.forapw.core.utils.mapper.InquiryMapper.buildInquiry;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InquiryService {

    private final InquiryRepository inquiryRepository;
    private final UserRepository userRepository;

    @Transactional
    public UserResponse.SubmitInquiryDTO submitInquiry(UserRequest.SubmitInquiry requestDTO, Long userId) {
        User submitter = userRepository.getReferenceById(userId);
        Inquiry inquiry = buildInquiry(requestDTO, InquiryStatus.PROCESSING, submitter);

        inquiryRepository.save(inquiry);

        return new UserResponse.SubmitInquiryDTO(inquiry.getId());
    }

    @Transactional
    public void updateInquiry(UserRequest.UpdateInquiry requestDTO, Long inquiryId, Long userId) {
        Inquiry inquiry = inquiryRepository.findById(inquiryId).orElseThrow(
                () -> new CustomException(ExceptionCode.INQUIRY_NOT_FOUND)
        );

        checkAuthority(userId, inquiry.getQuestioner());
        inquiry.updateInquiry(requestDTO.title(), requestDTO.description(), requestDTO.contactMail());
    }

    public UserResponse.FindInquiryListDTO findInquiries(Long userId) {
        List<Inquiry> inquiries = inquiryRepository.findAllByQuestionerId(userId);
        List<UserResponse.InquiryDTO> inquiryDTOS = inquiries.stream()
                .map(InquiryMapper::toInquiryDTO)
                .toList();

        return new UserResponse.FindInquiryListDTO(inquiryDTOS);
    }

    private void checkAuthority(Long userId, User writer) {
        if (writer.isNotSameUser(userId)) {
            throw new CustomException(ExceptionCode.USER_FORBIDDEN);
        }
    }
}
