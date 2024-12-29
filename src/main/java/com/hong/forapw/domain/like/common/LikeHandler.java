package com.hong.forapw.domain.like.common;

public interface LikeHandler {

    void validateBeforeLike(Long targetId, Long userId);

    boolean isAlreadyLiked(Long targetId, Long userId);

    void addLike(Long targetId, Long userId);

    void removeLike(Long targetId, Long userId);

    Long getLikeCount(Long targetId);

    String buildLockKey(Long targetId);
}