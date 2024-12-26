package com.hong.forapw.domain.post.model.request;

import jakarta.validation.constraints.NotBlank;

public record UpdateCommentReq(
        @NotBlank(message = "본문을 입력해주세요.")
        String content) {
}
