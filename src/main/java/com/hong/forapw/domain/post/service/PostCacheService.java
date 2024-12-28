package com.hong.forapw.domain.post.service;

import com.hong.forapw.domain.post.entity.Post;
import com.hong.forapw.integration.redis.RedisService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import static com.hong.forapw.common.constants.GlobalConstants.*;

@Service
@RequiredArgsConstructor
public class PostCacheService {

    private final RedisService redisService;

    private static final Long POST_CACHE_EXPIRATION = 1000L * 60 * 60 * 24 * 90; // 게시글 좋아요/뷰 카운트를 캐싱하는 기간 (3개월)
    private static final Long NOTICE_READ_EXPIRATION = 60L * 60 * 24 * 360; // 공지사항 읽음 상태 캐싱 기간 (1년)


    public void initializePostCache(Long postId) {
        redisService.storeValue(POST_LIKE_COUNT_KEY, postId.toString(), "0", POST_CACHE_EXPIRATION);
        redisService.storeValue(POST_VIEW_COUNT_KEY, postId.toString(), "0", POST_CACHE_EXPIRATION);
    }

    public void initializeCommentCache(Long commentId) {
        redisService.storeValue(COMMENT_LIKE_COUNT_KEY, commentId.toString(), "0", POST_CACHE_EXPIRATION);
    }

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
