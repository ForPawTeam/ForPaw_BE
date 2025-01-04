package com.hong.forapw.domain.like.handler;

import com.hong.forapw.common.exceptions.CustomException;
import com.hong.forapw.common.exceptions.ExceptionCode;
import com.hong.forapw.domain.group.entity.FavoriteGroup;
import com.hong.forapw.domain.group.entity.Group;
import com.hong.forapw.domain.group.model.query.GroupIdAndLikeCount;
import com.hong.forapw.domain.like.common.LikeHandler;
import com.hong.forapw.domain.like.common.Like;
import com.hong.forapw.domain.post.model.query.PostIdAndLikeCount;
import com.hong.forapw.domain.user.entity.User;
import com.hong.forapw.domain.user.repository.UserRepository;
import com.hong.forapw.domain.group.repository.FavoriteGroupRepository;
import com.hong.forapw.domain.group.repository.GroupRepository;
import com.hong.forapw.integration.redis.RedisService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.hong.forapw.common.constants.GlobalConstants.*;
import static com.hong.forapw.common.constants.GlobalConstants.POST_LIKE_NUM_KEY;

@Component
@RequiredArgsConstructor
public class GroupLikeHandler implements LikeHandler {

    private final GroupRepository groupRepository;
    private final UserRepository userRepository;
    private final FavoriteGroupRepository favoriteGroupRepository;
    private final RedisService redisService;

    public static final Long GROUP_CACHE_EXPIRATION_MS = 1000L * 60 * 60 * 24 * 90; // 세 달

    @Override
    public Like getLikeTarget() {
        return Like.GROUP;
    }

    @Override
    public void validateBeforeLike(Long groupId, Long userId) {
        if (!groupRepository.existsById(groupId)) {
            throw new CustomException(ExceptionCode.GROUP_NOT_FOUND);
        }
    }

    @Override
    public boolean isAlreadyLiked(Long groupId, Long userId) {
        return redisService.isMemberOfSet(buildUserLikedSetKey(userId), groupId.toString());
    }

    @Override
    public void addLike(Long groupId, Long userId) {
        Group group = groupRepository.getReferenceById(groupId);
        User user = userRepository.getReferenceById(userId);
        FavoriteGroup favoriteGroup = FavoriteGroup.builder()
                .user(user)
                .group(group)
                .build();

        favoriteGroupRepository.save(favoriteGroup);

        redisService.addSetElement(buildUserLikedSetKey(userId), groupId);
        redisService.incrementValue(GROUP_LIKE_NUM_KEY, groupId.toString(), 1L);
    }

    @Override
    public void removeLike(Long groupId, Long userId) {
        Optional<FavoriteGroup> favoriteGroupOP = favoriteGroupRepository.findByUserIdAndGroupId(userId, groupId);
        favoriteGroupOP.ifPresent(favoriteGroupRepository::delete);

        redisService.removeSetElement(buildUserLikedSetKey(userId), groupId.toString());
        redisService.decrementValue(GROUP_LIKE_NUM_KEY, groupId.toString(), 1L);
    }

    @Override
    public Long getLikeCount(Long groupId) {
        Long likeCount = redisService.getValueInLongWithNull(GROUP_LIKE_NUM_KEY, groupId.toString());
        if (likeCount == null) {
            likeCount = groupRepository.countLikesByGroupId(groupId);
            redisService.storeValue(GROUP_LIKE_NUM_KEY, groupId.toString(), likeCount.toString(), GROUP_CACHE_EXPIRATION_MS);
        }

        return likeCount;
    }

    @Override
    public Map<Long, Long> getLikesFromCache(List<Long> groupIds) {
        Map<Long, Long> result = new HashMap<>();
        for (Long groupId : groupIds) {
            Long likeCount = redisService.getValueInLongWithNull(GROUP_LIKE_NUM_KEY, groupId.toString());
            if (likeCount != null) {
                result.put(groupId, likeCount);
            }
        }
        return result;
    }

    @Override
    public Map<Long, Long> getLikesFromDatabaseAndCache(List<Long> missingIds) {
        Map<Long, Long> dbLikes = new HashMap<>();
        List<GroupIdAndLikeCount> dbResults = groupRepository.findLikeCountsByIds(missingIds);

        for (GroupIdAndLikeCount row : dbResults) {
            Long groupId = row.groupId();
            Long likeCount = row.likeCount();
            dbLikes.put(groupId, likeCount);
            redisService.storeValue(POST_LIKE_NUM_KEY, groupId.toString(), likeCount.toString(), GROUP_CACHE_EXPIRATION_MS);
        }
        return dbLikes;
    }

    @Override
    public String buildLockKey(Long groupId) {
        return "group:" + groupId + ":like:lock";
    }

    public void initCount(Long groupId) {
        redisService.storeValue(GROUP_LIKE_NUM_KEY, groupId.toString(), "0");
    }

    public void clear(Long groupId) {
        redisService.removeValue(GROUP_LIKE_NUM_KEY, groupId.toString());
    }

    private String buildUserLikedSetKey(Long userId) {
        return String.format(GROUP_LIKED_SET_KEY, userId);
    }
}