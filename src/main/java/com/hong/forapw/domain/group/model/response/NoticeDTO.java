package com.hong.forapw.domain.group.model.response;

import com.hong.forapw.domain.post.entity.Post;

import java.time.LocalDateTime;

public record NoticeDTO(
        Long id,
        String name,
        LocalDateTime date,
        String title,
        Boolean isRead
) {

    public NoticeDTO(Post notice, boolean isRead) {
        this(
                notice.getId(),
                notice.getWriterNickName(),
                notice.getCreatedDate(),
                notice.getTitle(),
                isRead
        );
    }
}