package com.hong.forapw.domain.search.model.response;

import com.hong.forapw.domain.post.constant.PostType;

import java.time.LocalDateTime;

public record PostDTO(
        Long id,
        PostType type,
        String title,
        String content,
        LocalDateTime date,
        String imageURL,
        String nickName,
        Long commentNum,
        Long likeNum) {
}