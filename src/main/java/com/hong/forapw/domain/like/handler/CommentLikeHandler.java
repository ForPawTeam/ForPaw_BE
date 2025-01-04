package com.hong.forapw.domain.like.handler;

import com.hong.forapw.common.exceptions.CustomException;
import com.hong.forapw.common.exceptions.ExceptionCode;
import com.hong.forapw.domain.animal.model.query.AnimalIdAndLikeCount;
import com.hong.forapw.domain.like.common.LikeHandler;
import com.hong.forapw.domain.like.common.Like;
import com.hong.forapw.domain.post.entity.Comment;
import com.hong.forapw.domain.post.entity.CommentLike;
import com.hong.forapw.domain.post.model.query.CommentIdAndLikeCount;
import com.hong.forapw.domain.user.entity.User;
import com.hong.forapw.domain.post.repository.CommentLikeRepository;
import com.hong.forapw.domain.post.repository.CommentRepository;
import com.hong.forapw.domain.user.repository.UserRepository;
import com.hong.forapw.integration.redis.RedisService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.hong.forapw.common.constants.GlobalConstants.*;
import static com.hong.forapw.common.constants.GlobalConstants.ANIMAL_LIKE_NUM_KEY;

@Component
@RequiredArgsConstructor
public class CommentLikeHandler implements LikeHandler {

    private final CommentRepository commentRepository;
    private final CommentLikeRepository commentLikeRepository;
    private final UserRepository userRepository;
    private final RedisService redisService;

    private static final Long POST_CACHE_EXPIRATION_MS = 1000L * 60 * 60 * 24 * 90;

    @Override
    public Like getLikeTarget() {
        return Like.COMMENT;
    }

    @Override
    public void validateBeforeLike(Long commentId, Long userId) {
        if (!commentRepository.existsById(commentId))
            throw new CustomException(ExceptionCode.COMMENT_NOT_FOUND);

        Long ownerId = findOwnerId(commentId);
        validateNotSelfLike(ownerId, userId);
    }

    @Override
    public boolean isAlreadyLiked(Long commentId, Long userId) {
        return redisService.isMemberOfSet(buildUserLikedSetKey(userId), commentId.toString());
    }

    @Override
    public void addLike(Long commentId, Long userId) {
        User user = userRepository.getReferenceById(userId);
        Comment comment = commentRepository.getReferenceById(commentId);
        CommentLike commentLike = CommentLike.builder()
                .user(user)
                .comment(comment)
                .build();

        commentLikeRepository.save(commentLike);

        redisService.addSetElement(buildUserLikedSetKey(userId), commentId);
        redisService.incrementValue(COMMENT_LIKE_NUM_KEY, commentId.toString(), 1L);
    }

    @Override
    public void removeLike(Long commentId, Long userId) {
        Optional<CommentLike> commentLikeOP = commentLikeRepository.findByUserIdAndCommentId(userId, commentId);
        commentLikeOP.ifPresent(commentLikeRepository::delete);

        redisService.removeSetElement(buildUserLikedSetKey(userId), commentId.toString());
        redisService.decrementValue(COMMENT_LIKE_NUM_KEY, commentId.toString(), 1L);
    }

    @Override
    public Long getLikeCount(Long commentId) {
        Long likeCount = redisService.getValueInLongWithNull(COMMENT_LIKE_NUM_KEY, commentId.toString());
        if (likeCount == null) {
            likeCount = commentRepository.countLikesByCommentId(commentId);
            redisService.storeValue(COMMENT_LIKE_NUM_KEY, commentId.toString(), likeCount.toString(), POST_CACHE_EXPIRATION_MS);
        }

        return likeCount;
    }

    @Override
    public Map<Long, Long> getLikesFromCache(List<Long> commentIds) {
        Map<Long, Long> result = new HashMap<>();
        for (Long commentId : commentIds) {
            Long likeCount = redisService.getValueInLongWithNull(COMMENT_LIKE_NUM_KEY, commentId.toString());
            if (likeCount != null) {
                result.put(commentId, likeCount);
            }
        }
        return result;
    }

    @Override
    public Map<Long, Long> getLikesFromDatabaseAndCache(List<Long> missingIds) {
        Map<Long, Long> dbLikes = new HashMap<>();
        List<CommentIdAndLikeCount> dbResults = commentRepository.findLikeCountsByIds(missingIds);

        for (CommentIdAndLikeCount row : dbResults) {
            Long commentId = row.commentId();
            Long likeCount = row.likeCount();
            dbLikes.put(commentId, likeCount);
            redisService.storeValue(COMMENT_LIKE_NUM_KEY, commentId.toString(), likeCount.toString(), POST_CACHE_EXPIRATION_MS);
        }
        return dbLikes;
    }

    @Override
    public String buildLockKey(Long commentId) {
        return "comment:" + commentId + ":like:lock";
    }

    private String buildUserLikedSetKey(Long userId) {
        return String.format(COMMENT_LIKED_SET_KEY, userId);
    }

    private Long findOwnerId(Long commentId) {
        return commentRepository.findUserIdById(commentId)
                .orElseThrow(() -> new CustomException(ExceptionCode.COMMENT_NOT_FOUND));
    }

    private void validateNotSelfLike(Long ownerId, Long userId) {
        if (ownerId.equals(userId)) {
            throw new CustomException(ExceptionCode.CANNOT_LIKE_OWN_POST);
        }
    }
}