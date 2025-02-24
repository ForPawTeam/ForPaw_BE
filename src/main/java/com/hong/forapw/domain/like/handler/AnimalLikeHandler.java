package com.hong.forapw.domain.like.handler;

import com.hong.forapw.common.exceptions.CustomException;
import com.hong.forapw.common.exceptions.ExceptionCode;
import com.hong.forapw.domain.animal.entity.Animal;
import com.hong.forapw.domain.animal.entity.FavoriteAnimal;
import com.hong.forapw.domain.animal.model.query.AnimalIdAndLikeCount;
import com.hong.forapw.domain.like.common.LikeHandler;
import com.hong.forapw.domain.like.common.Like;
import com.hong.forapw.domain.user.entity.User;
import com.hong.forapw.domain.user.repository.UserRepository;
import com.hong.forapw.domain.animal.repository.AnimalRepository;
import com.hong.forapw.domain.animal.repository.FavoriteAnimalRepository;
import com.hong.forapw.integration.redis.RedisService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.hong.forapw.common.constants.GlobalConstants.*;

@Component
@RequiredArgsConstructor
public class AnimalLikeHandler implements LikeHandler {

    private final RedisService redisService;
    private final FavoriteAnimalRepository favoriteAnimalRepository;
    private final AnimalRepository animalRepository;
    private final UserRepository userRepository;

    @Override
    public Like getLikeTarget() {
        return Like.ANIMAL;
    }

    @Override
    public void validateBeforeLike(Long animalId, Long userId) {
        if (!animalRepository.existsById(animalId))
            throw new CustomException(ExceptionCode.ANIMAL_NOT_FOUND);
    }

    @Override
    public boolean isAlreadyLiked(Long animalId, Long userId) {
        return redisService.isMemberOfSet(buildUserLikedSetKey(userId), animalId.toString());
    }

    @Override
    public void addLike(Long animalId, Long userId) {
        Animal animal = animalRepository.getReferenceById(animalId);
        User user = userRepository.getReferenceById(userId);
        FavoriteAnimal favoriteAnimal = FavoriteAnimal.builder()
                .user(user)
                .animal(animal)
                .build();

        favoriteAnimalRepository.save(favoriteAnimal);

        redisService.addSetElement(buildUserLikedSetKey(userId), animalId);
        redisService.incrementValue(ANIMAL_LIKE_NUM_KEY, animalId.toString(), 1L);
    }

    @Override
    public void removeLike(Long animalId, Long userId) {
        Optional<FavoriteAnimal> favoriteAnimalOP = favoriteAnimalRepository.findByUserIdAndAnimalId(userId, animalId);
        favoriteAnimalOP.ifPresent(favoriteAnimalRepository::delete);

        redisService.removeSetElement(buildUserLikedSetKey(userId), animalId.toString());
        redisService.decrementValue(ANIMAL_LIKE_NUM_KEY, animalId.toString(), 1L);
    }

    @Override
    public Long getLikeCount(Long animalId) {
        Long likeCount = redisService.getValueInLongWithNull(ANIMAL_LIKE_NUM_KEY, animalId.toString());
        if (likeCount == null) {
            likeCount = animalRepository.countLikesByAnimalId(animalId);
            redisService.storeValue(ANIMAL_LIKE_NUM_KEY, animalId.toString(), likeCount.toString());
        }

        return likeCount;
    }

    @Override
    public Map<Long, Long> getLikesFromCache(List<Long> animalIds) {
        Map<Long, Long> result = new HashMap<>();
        for (Long animalId : animalIds) {
            Long likeCount = redisService.getValueInLongWithNull(ANIMAL_LIKE_NUM_KEY, animalId.toString());
            if (likeCount != null) {
                result.put(animalId, likeCount);
            }
        }
        return result;
    }

    @Override
    public Map<Long, Long> getLikesFromDatabaseAndCache(List<Long> missingIds) {
        Map<Long, Long> dbLikes = new HashMap<>();
        List<AnimalIdAndLikeCount> dbResults = animalRepository.findLikeCountsByIds(missingIds);
        for (AnimalIdAndLikeCount row : dbResults) {
            Long animalId = row.animalId();
            Long likeCount = row.likeCount();
            dbLikes.put(animalId, likeCount);
            redisService.storeValue(ANIMAL_LIKE_NUM_KEY, animalId.toString(), likeCount.toString());
        }
        return dbLikes;
    }

    @Override
    public String buildLockKey(Long animalId) {
        return "animal:" + animalId + ":like:lock";
    }

    @Override
    public void clear(Long animalId) {
        redisService.removeValue(ANIMAL_LIKE_NUM_KEY, animalId.toString());
    }

    private String buildUserLikedSetKey(Long userId) {
        return String.format(ANIMAL_LIKED_SET_KEY, userId);
    }
}