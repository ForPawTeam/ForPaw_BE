package com.hong.forapw.domain.post.model.query;

import java.time.LocalDateTime;

public interface PostProjection {
    Long getPostId();
    String getTitle();
    String getContent();
    LocalDateTime getCreatedDate();
    String getPostType();
    String getImageUrl();
    Long getUserId();
    String getNickName();
    Long getCommentNum();
}