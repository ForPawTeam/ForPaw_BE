package com.hong.forapw.domain.post;

import com.hong.forapw.admin.constant.ContentType;
import com.hong.forapw.admin.repository.ReportRepository;
import com.hong.forapw.common.exceptions.CustomException;
import com.hong.forapw.common.exceptions.ExceptionCode;
import com.hong.forapw.domain.post.constant.PostType;
import com.hong.forapw.domain.post.model.request.CreatePostReq;
import com.hong.forapw.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PostValidator {

    private final ReportRepository reportRepository;

    public void validatePostRequest(CreatePostReq request) {
        if (request.type() == PostType.ANSWER) {
            throw new CustomException(ExceptionCode.NOT_QUESTION_TYPE);
        }

        if (request.type().isImageRequired() && request.images().isEmpty()) {
            throw new CustomException(ExceptionCode.POST_MUST_CONTAIN_IMAGE);
        }
    }

    public void validateReportRequest(Long contentId, ContentType contentType, Long userId) {
        if (isAlreadyReported(contentId, contentType, userId)) {
            throw new CustomException(ExceptionCode.REPORT_DUPLICATE);
        }

        if (contentType.isNotValidTypeForReport()) {
            throw new CustomException(ExceptionCode.INVALID_REPORT_TARGET);
        }
    }

    public void validateNotSelfReport(Long userId, User reportedUser) {
        if (reportedUser.isSameUser(userId)) {
            throw new CustomException(ExceptionCode.CANNOT_REPORT_OWN_CONTENT);
        }
    }

    public void validateAccessorAuthorization(User accessor, Long writerId) {
        if (accessor.isAdmin()) {
            return;
        }
        if (accessor.isNotSameUser(writerId)) {
            throw new CustomException(ExceptionCode.UNAUTHORIZED_ACCESS);
        }
    }

    private boolean isAlreadyReported(Long contentId, ContentType contentType, Long userId) {
        return reportRepository.existsByReporterIdAndContentId(userId, contentId, contentType);
    }
}
