package com.hong.forapw.domain.like.handler;

import com.hong.forapw.common.exceptions.CustomException;
import com.hong.forapw.common.exceptions.ExceptionCode;
import com.hong.forapw.domain.like.common.LikeHandler;
import com.hong.forapw.domain.post.entity.Post;
import com.hong.forapw.domain.post.entity.PostLike;
import com.hong.forapw.domain.user.entity.User;
import com.hong.forapw.domain.post.repository.PostLikeRepository;
import com.hong.forapw.domain.post.repository.PostRepository;
import com.hong.forapw.domain.user.repository.UserRepository;
import com.hong.forapw.integration.redis.RedisService;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

import static com.hong.forapw.common.constants.GlobalConstants.POST_LIKED_SET_KEY;
import static com.hong.forapw.common.constants.GlobalConstants.POST_LIKE_NUM_KEY;

@Component
@RequiredArgsConstructor
public class PostLikeHandler implements LikeHandler {

    private final PostRepository postRepository;
    private final PostLikeRepository postLikeRepository;
    private final UserRepository userRepository;
    private final RedisService redisService;

    private static final Long POST_CACHE_EXPIRATION_MS = 1000L * 60 * 60 * 24 * 90;


    @Override
    public void validateBeforeLike(Long postId, Long userId) {
        if(!postRepository.existsById(postId))
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
    }

    @Override
    public void removeLike(Long postId, Long userId) {
        Optional<PostLike> postLikeOP = postLikeRepository.findByUserIdAndPostId(userId, postId);
        postLikeOP.ifPresent(postLikeRepository::delete);

        redisService.removeSetElement(buildUserLikedSetKey(userId), postId.toString());
        redisService.decrementValue(POST_LIKE_NUM_KEY, postId.toString(), 1L);
    }

    @Override
    public Long getLikeCount(Long postId) {
        Long likeCount = redisService.getValueInLongWithNull(POST_LIKE_NUM_KEY, postId.toString());
        if (likeCount == null) {
            likeCount = postRepository.countLikesByPostId(postId);
            redisService.storeValue(POST_LIKE_NUM_KEY, postId.toString(), likeCount.toString(), POST_CACHE_EXPIRATION_MS);
        }

        return likeCount;
    }

    @Override
    public String buildLockKey(Long postId) {
        return "post:" + postId + ":like:lock";
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
}