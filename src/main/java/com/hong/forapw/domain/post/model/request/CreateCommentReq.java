package com.hong.forapw.domain.post.model.request;

import jakarta.validation.constraints.NotBlank;

public record CreateCommentReq(
        @NotBlank(message = "댓글을 입력해주세요.")
        String content) {
}
