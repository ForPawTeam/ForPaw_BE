package com.hong.forapw.domain.like.common;

import java.util.List;
import java.util.Map;

public interface LikeHandler {

    Like getLikeTarget();

    void validateBeforeLike(Long targetId, Long userId);

    boolean isAlreadyLiked(Long targetId, Long userId);

    void addLike(Long targetId, Long userId);

    void removeLike(Long targetId, Long userId);

    Long getLikeCount(Long targetId);

    Map<Long, Long> getLikesFromCache(List<Long> targetIds);

    Map<Long, Long> getLikesFromDatabaseAndCache(List<Long> missingIds);

    String buildLockKey(Long targetId);

    void clear(Long id);
}