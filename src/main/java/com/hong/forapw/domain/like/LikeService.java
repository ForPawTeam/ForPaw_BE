package com.hong.forapw.domain.like;

import com.hong.forapw.domain.like.common.LikeHandler;
import com.hong.forapw.domain.like.common.Like;
import com.hong.forapw.integration.redis.RedisService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RLock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LikeService {

    private final RedisService redisService;
    private final List<LikeHandler> handlers;
    private Map<Like, LikeHandler> handlerMap;

    @PostConstruct
    void initHandlerMap() {
        handlerMap = new EnumMap<>(Like.class);
        for (LikeHandler handler : handlers) {
            handlerMap.put(handler.getLikeTarget(), handler);
        }
    }

    @Transactional
    public void like(Long targetId, Long userId, Like target) {
        LikeHandler handler = handlerMap.get(target);
        handler.validateBeforeLike(targetId, userId);
        executeWithLock(handler.buildLockKey(targetId), () -> toggleLike(handler, targetId, userId));
    }

    public Long getLikeCount(Long targetId, Like target) {
        LikeHandler handler = handlerMap.get(target);
        return handler.getLikeCount(targetId);
    }

    public Map<Long, Long> getLikeCounts(List<Long> targetIds, Like target) {
        LikeHandler handler = handlerMap.get(target);
        return handler.getLikeCountMap(targetIds);
    }

    public void clearLikeCounts(Long targetId, Like target) {
        LikeHandler handler = handlerMap.get(target);
        handler.clear(targetId);
    }

    private void executeWithLock(String lockKey, Runnable action) {
        RLock lock = redisService.getLock(lockKey);
        try {
            redisService.tryToAcquireLock(lock, 2, 5, TimeUnit.SECONDS);
            action.run();
        } finally {
            redisService.safelyReleaseLock(lock);
        }
    }

    private void toggleLike(LikeHandler handler, Long targetId, Long userId) {
        if (handler.isAlreadyLiked(targetId, userId)) {
            handler.removeLike(targetId, userId);
        } else {
            handler.addLike(targetId, userId);
        }
    }
}