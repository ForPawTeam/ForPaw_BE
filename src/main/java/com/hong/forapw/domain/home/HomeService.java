package com.hong.forapw.domain.home;

import com.hong.forapw.domain.animal.AnimalService;
import com.hong.forapw.domain.animal.entity.Animal;
import com.hong.forapw.domain.group.model.response.RecommendGroupDTO;
import com.hong.forapw.domain.home.model.response.FindHomeRes;
import com.hong.forapw.domain.like.common.Like;
import com.hong.forapw.domain.post.entity.PopularPost;
import com.hong.forapw.domain.post.entity.Post;
import com.hong.forapw.domain.animal.repository.AnimalRepository;
import com.hong.forapw.domain.group.repository.FavoriteGroupRepository;
import com.hong.forapw.domain.post.repository.PopularPostRepository;
import com.hong.forapw.domain.group.service.GroupService;
import com.hong.forapw.domain.like.LikeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.hong.forapw.common.constants.GlobalConstants.DEFAULT_PROVINCE;
import static com.hong.forapw.common.utils.PaginationUtils.POPULAR_POST_PAGEABLE;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HomeService {

    private final AnimalRepository animalRepository;
    private final PopularPostRepository popularPostRepository;
    private final GroupService groupService;
    private final LikeService likeService;
    private final AnimalService animalService;
    private final FavoriteGroupRepository favoriteGroupRepository;

    public FindHomeRes findHomePageData(Long userId) {
        List<FindHomeRes.AnimalDTO> recommendedAnimals = getRecommendedAnimals(userId);
        List<FindHomeRes.PostDTO> popularPosts = getPopularPosts();
        List<RecommendGroupDTO> recommendedGroups = getRecommendedGroups(userId);

        return new FindHomeRes(recommendedAnimals, recommendedGroups, popularPosts);
    }

    private List<FindHomeRes.AnimalDTO> getRecommendedAnimals(Long userId) {
        List<Long> recommendedAnimalIds = animalService.findRecommendedAnimalIds(userId);
        List<Animal> animals = animalRepository.findByIds(recommendedAnimalIds);
        List<Long> animalIds = animals.stream().map(Animal::getId).toList();

        Map<Long, Long> likeCountMap = likeService.getLikeCounts(animalIds, Like.ANIMAL);
        return FindHomeRes.AnimalDTO.fromEntities(animals, likeCountMap);
    }

    private List<FindHomeRes.PostDTO> getPopularPosts() {
        List<Post> popularPosts = popularPostRepository.findAllWithPost(POPULAR_POST_PAGEABLE).stream()
                .map(PopularPost::getPost)
                .toList();
        List<Long> postIds = popularPosts.stream().map(Post::getId).toList();

        Map<Long, Long> likeCountMap = likeService.getLikeCounts(postIds, Like.POST);
        return FindHomeRes.PostDTO.fromEntities(popularPosts, likeCountMap);
    }

    private List<RecommendGroupDTO> getRecommendedGroups(Long userId) {
        List<Long> likedGroupIds = Optional.ofNullable(userId)
                .map(favoriteGroupRepository::findGroupIdByUserId)
                .orElse(Collections.emptyList());

        return groupService.getRecommendGroups(userId, DEFAULT_PROVINCE, likedGroupIds);
    }
}