package com.hong.forapw.domain.group.model.request;

import com.hong.forapw.domain.group.entity.Group;
import com.hong.forapw.domain.post.constant.PostType;
import com.hong.forapw.domain.post.entity.Post;
import com.hong.forapw.domain.user.entity.User;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record CreateNoticeReq(
        @NotBlank(message = "제목을 입력해주세요.")
        String title,
        @NotBlank(message = "본문을 입력해주세요.")
        String content,
        List<PostImageDTO> images
) {
    public Post toEntity(User noticer, Group group) {
        return Post.builder()
                .user(noticer)
                .group(group)
                .postType(PostType.NOTICE)
                .title(title)
                .content(content)
                .build();
    }

    public record PostImageDTO(String imageURL) {
    }
}