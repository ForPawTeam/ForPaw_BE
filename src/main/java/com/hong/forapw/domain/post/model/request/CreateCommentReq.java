package com.hong.forapw.domain.post.model.request;

import com.hong.forapw.domain.post.entity.Comment;
import com.hong.forapw.domain.post.entity.Post;
import com.hong.forapw.domain.user.entity.User;
import jakarta.validation.constraints.NotBlank;

public record CreateCommentReq(
        @NotBlank(message = "댓글을 입력해주세요.")
        String content
) {
    public Comment toEntity(String content, Post post, User writer) {
        return Comment.builder()
                .user(writer)
                .post(post)
                .content(content)
                .build();
    }
}
