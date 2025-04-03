package com.hong.forapw.domain.like.handler;

import com.hong.forapw.common.exceptions.CustomException;
import com.hong.forapw.common.exceptions.ExceptionCode;
import com.hong.forapw.domain.like.common.LikeHandler;
import com.hong.forapw.domain.like.common.Like;
import com.hong.forapw.domain.post.entity.Post;
import com.hong.forapw.domain.post.entity.PostLike;
import com.hong.forapw.domain.user.entity.User;
import com.hong.forapw.domain.post.repository.PostLikeRepository;
import com.hong.forapw.domain.post.repository.PostRepository;
import com.hong.forapw.domain.user.repository.UserRepository;
import com.hong.forapw.integration.redis.RedisService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.hong.forapw.integration.redis.RedisConstants.POST_LIKED_SET_KEY;
import static com.hong.forapw.integration.redis.RedisConstants.POST_LIKE_NUM_KEY;

@Component
@RequiredArgsConstructor
public class PostLikeHandler implements LikeHandler {

    private final PostRepository postRepository;
    private final PostLikeRepository postLikeRepository;
    private final UserRepository userRepository;
    private final RedisService redisService;

    // 좋아요 시간 추적을 위한 정렬된 셋 키 프리픽스
    private static final String POST_LIKE_TIMESTAMP_KEY = "post:like:timestamps";

    @Override
    public Like getLikeTarget() {
        return Like.POST;
    }

    @Override
    public void validateBeforeLike(Long postId, Long userId) {
        if (!postRepository.existsById(postId))
            throw new CustomException(ExceptionCode.POST_NOT_FOUND);

        Long ownerId = findOwnerId(postId);
        validateNotSelfLike(ownerId, userId);
    }

    @Override
    public boolean isAlreadyLiked(Long postId, Long userId) {
        return redisService.isMemberOfSet(buildUserLikedSetKey(userId), postId.toString());
    }

    @Override
    public void addLike(Long postId, Long userId) {
        User user = userRepository.getReferenceById(userId);
        Post post = postRepository.getReferenceById(postId);
        PostLike postLike = PostLike.builder()
                .user(user)
                .post(post)
                .build();

        postLikeRepository.save(postLike);

        redisService.addSetElement(buildUserLikedSetKey(userId), postId);
        redisService.incrementValue(POST_LIKE_NUM_KEY, postId.toString(), 1L);

        String member = buildTimestampMember(postId, userId);
        double score = getCurrentTimestampInSeconds();
        redisService.addSortedSetElement(POST_LIKE_TIMESTAMP_KEY, member, score);
    }

    @Override
    public void removeLike(Long postId, Long userId) {
        Optional<PostLike> postLikeOP = postLikeRepository.findByUserIdAndPostId(userId, postId);
        postLikeOP.ifPresent(postLikeRepository::delete);

        redisService.removeSetElement(buildUserLikedSetKey(userId), postId.toString());
        redisService.decrementValue(POST_LIKE_NUM_KEY, postId.toString(), 1L);

        String member = buildTimestampMember(postId, userId);
        redisService.removeSortedSetElement(POST_LIKE_TIMESTAMP_KEY, member);
    }

    @Override
    public Long getLikeCount(Long postId) {
        return redisService.getValueInLong(POST_LIKE_NUM_KEY, postId.toString());
    }

    @Override
    public Map<Long, Long> getLikeCountMap(List<Long> postIds) {
        Map<Long, Long> countMap = new HashMap<>();
        for (Long postId : postIds) {
            Long likeCount = redisService.getValueInLong(POST_LIKE_NUM_KEY, postId.toString());
            countMap.put(postId, likeCount);
        }
        return countMap;
    }

    public long countByPostIdAndCreatedDateAfter(Long postId, LocalDateTime dateTime) {
        double minScore = dateTime.atZone(ZoneId.systemDefault()).toInstant().getEpochSecond();
        double maxScore = Double.POSITIVE_INFINITY; // 최대값까지 (현재까지)
        String prefix = postId + ":";

        return redisService.countSortedSetElementsInRange(POST_LIKE_TIMESTAMP_KEY, prefix, minScore, maxScore);
    }

    @Override
    public String buildLockKey(Long postId) {
        return "post:" + postId + ":like:lock";
    }

    @Override
    public void clear(Long postId) {
        redisService.removeValue(POST_LIKE_NUM_KEY, postId.toString());

        String prefix = postId + ":";
        redisService.removeSortedSetElementsByPrefix(POST_LIKE_TIMESTAMP_KEY, prefix);
    }

    private String buildUserLikedSetKey(Long userId) {
        return String.format(POST_LIKED_SET_KEY, userId);
    }

    private Long findOwnerId(Long postId) {
        return postRepository.findUserIdById(postId)
                .orElseThrow(() -> new CustomException(ExceptionCode.POST_NOT_FOUND));
    }

    private void validateNotSelfLike(Long ownerId, Long userId) {
        if (ownerId.equals(userId)) {
            throw new CustomException(ExceptionCode.CANNOT_LIKE_OWN_POST);
        }
    }

    private String buildTimestampMember(Long postId, Long userId) {
        return postId + ":" + userId;
    }

    private double getCurrentTimestampInSeconds() {
        return Instant.now().getEpochSecond();
    }
}