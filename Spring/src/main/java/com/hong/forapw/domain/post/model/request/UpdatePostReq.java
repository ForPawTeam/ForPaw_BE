package com.hong.forapw.domain.post.model.request;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record UpdatePostReq(
        @NotBlank(message = "제목을 입력해주세요.")
        String title,
        @NotBlank(message = "본문을 입력해주세요.")
        String content,
        List<Long> retainedImageIds,
        List<PostImageDTO> newImages) {
}