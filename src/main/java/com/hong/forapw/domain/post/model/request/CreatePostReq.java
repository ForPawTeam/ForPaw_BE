package com.hong.forapw.domain.post.model.request;

import com.hong.forapw.domain.post.constant.PostType;
import com.hong.forapw.domain.post.model.PostImageDTO;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record CreatePostReq(
        @NotBlank(message = "제목을 입력해주세요.")
        String title,
        @NotNull(message = "글의 종류를 선택해주세요")
        PostType type,
        @NotBlank(message = "본문을 입력해주세요.")
        String content,
        @NotNull(message = "이미지가 비어있다면, null이 아닌 빈 리스트로 보내주세요.")
        List<PostImageDTO> images) {
}
