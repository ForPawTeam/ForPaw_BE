package com.hong.forapw.domain.post;

import com.hong.forapw.admin.constant.ContentType;
import com.hong.forapw.admin.repository.ReportRepository;
import com.hong.forapw.common.exceptions.CustomException;
import com.hong.forapw.common.exceptions.ExceptionCode;
import com.hong.forapw.domain.post.constant.PostType;
import com.hong.forapw.domain.post.entity.Comment;
import com.hong.forapw.domain.post.entity.Post;
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

    public void validateQuestionType(Post question) {
        if (question.isNotQuestionType()) {
            throw new CustomException(ExceptionCode.NOT_QUESTION_TYPE);
        }
    }

    public void validatePost(Post post) {
        if (post.isQuestionType()) {
            throw new CustomException(ExceptionCode.NOT_QUESTION_TYPE);
        }

        if (post.isScreened()) {
            throw new CustomException(ExceptionCode.SCREENED_POST);
        }
    }

    public void validateQna(Post qna) {
        if (qna.isNotQuestionType()) {
            throw new CustomException(ExceptionCode.NOT_QUESTION_TYPE);
        }

        if (qna.isScreened()) {
            throw new CustomException(ExceptionCode.SCREENED_POST);
        }
    }

    public void validateAnswer(Post answer) {
        if (answer.isNotAnswerType()) {
            throw new CustomException(ExceptionCode.NOT_ANSWER_TYPE);
        }
    }

    public void validateParentComment(Comment parentComment, Long postId) {
        if (parentComment.isReply()) {
            throw new CustomException(ExceptionCode.CANT_REPLY_TO_REPLY);
        }

        if (parentComment.isNotBelongToPost(postId)) {
            throw new CustomException(ExceptionCode.NOT_POSTS_COMMENT);
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

    public void validateCommentBelongsToPost(Comment comment, Long postId) {
        if (comment.isNotBelongToPost(postId)) {
            throw new CustomException(ExceptionCode.NOT_POSTS_COMMENT);
        }
    }

    private boolean isAlreadyReported(Long contentId, ContentType contentType, Long userId) {
        return reportRepository.existsByReporterIdAndContentId(userId, contentId, contentType);
    }
}
