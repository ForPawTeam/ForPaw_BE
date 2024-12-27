package com.hong.forapw.domain.post.model.response;

import com.hong.forapw.domain.post.entity.Comment;
import org.springframework.data.domain.Page;

import java.time.LocalDateTime;
import java.util.List;

public record FindMyCommentListRes(
        List<MyCommentDTO> comments,
        boolean isLastPage
) {
    public FindMyCommentListRes(Page<Comment> myCommentPage) {
        this(
                myCommentPage.getContent().stream()
                        .map(MyCommentDTO::new)
                        .toList(),
                myCommentPage.isLast()
        );
    }

    public record MyCommentDTO(
            Long commentId,
            Long postId,
            String postType,
            String content,
            LocalDateTime date,
            String title,
            Long commentNum,
            boolean isBlocked) {

        public MyCommentDTO(Comment comment) {
            this(
                    comment.getId(),
                    comment.getPostId(),
                    comment.getPostTypeValue(),
                    comment.getContent(),
                    comment.getCreatedDate(),
                    comment.getPostTitle(),
                    comment.getPostCommentNumber(),
                    comment.isPostBlocked()
            );
        }
    }
}
