package com.hong.forapw.domain.post.service;

import com.hong.forapw.domain.post.entity.Post;
import com.hong.forapw.integration.redis.RedisService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import static com.hong.forapw.common.constants.GlobalConstants.*;
import static com.hong.forapw.integration.redis.RedisConstants.*;

@Service
@RequiredArgsConstructor
public class PostCacheService {

    private final RedisService redisService;

    public void markNoticePostAsRead(Post post, Long userId, Long postId) {
        if (post.isNoticeType()) {
            String key = POST_READ_KEY + userId;
            redisService.addSetElement(key, postId, NOTICE_READ_EXPIRATION);
        }
    }

    public void incrementPostViewCount(Long postId) {
        redisService.incrementValue(POST_VIEW_COUNT_KEY, postId.toString(), 1L);
    }

    public Long getPostViewCount(Long postId, Post post) {
        Long viewCount = redisService.getValueInLongWithNull(POST_VIEW_COUNT_KEY, postId.toString());
        if (viewCount == null) {
            viewCount = post.getReadCnt();
            redisService.storeValue(POST_VIEW_COUNT_KEY, postId.toString(), viewCount.toString(), POST_CACHE_EXPIRATION);
        }
        return viewCount;
    }

    public Long getPostViewCount(Post post) {
        return redisService.getValueInLongWithNull(POST_VIEW_COUNT_KEY, post.getId().toString());
    }
}
