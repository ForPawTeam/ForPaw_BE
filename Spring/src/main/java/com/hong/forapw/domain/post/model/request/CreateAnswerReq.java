package com.hong.forapw.domain.post.model.request;

import com.hong.forapw.domain.post.constant.PostType;
import com.hong.forapw.domain.post.entity.Post;
import com.hong.forapw.domain.user.entity.User;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record CreateAnswerReq(
        @NotBlank(message = "답변을 입력해주세요.")
        String content,
        List<PostImageDTO> images
) {
        public Post toEntity(User owner, Post questionPost) {
                return Post.builder()
                        .user(owner)
                        .postType(PostType.ANSWER)
                        .title(questionPost.getTitle() + "(답변)")
                        .content(content)
                        .build();
        }
}