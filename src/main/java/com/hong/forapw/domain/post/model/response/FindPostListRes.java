package com.hong.forapw.domain.post.model.response;

import java.time.LocalDateTime;
import java.util.List;

public record FindPostListRes(
        List<PostDTO> posts,
        boolean isLastPage) {

    public record PostDTO(
            Long id,
            String nickName,
            String title,
            String content,
            LocalDateTime date,
            Long commentNum,
            Long likeNum,
            String imageURL,
            boolean isBlocked) {
    }
}