package com.hong.forapw.domain.post.model.request;

import com.hong.forapw.domain.post.model.PostImageDTO;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record CreateAnswerReq(
        @NotBlank(message = "답변을 입력해주세요.")
        String content,
        List<PostImageDTO> images) {
}
