package com.hong.forapw.domain.home;

import com.hong.forapw.domain.animal.AnimalService;
import com.hong.forapw.domain.home.model.response.FindHomeRes;
import com.hong.forapw.domain.post.entity.PopularPost;
import com.hong.forapw.domain.region.constant.Province;
import com.hong.forapw.domain.post.entity.Post;
import com.hong.forapw.domain.animal.repository.AnimalRepository;
import com.hong.forapw.domain.group.repository.FavoriteGroupRepository;
import com.hong.forapw.domain.post.repository.PopularPostRepository;
import com.hong.forapw.domain.group.service.GroupService;
import com.hong.forapw.domain.like.LikeService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

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

    private static final Province DEFAULT_PROVINCE = Province.DAEGU;
    private static final String SORT_BY_DATE = "createdDate";
    private static final int POPULAR_POST_PAGE_INDEX = 0;
    private static final int POPULAR_POST_PAGE_SIZE = 5;

    public FindHomeRes findHomePageData(Long userId) {
        List<FindHomeRes.AnimalDTO> recommendedAnimals = findRecommendedAnimals(userId);
        List<FindHomeRes.PostDTO> popularPosts = findPopularPosts();
        List<FindHomeRes.RecommendGroupDTO> recommendedGroups = findRecommendedGroups(userId);

        return new FindHomeRes(recommendedAnimals, recommendedGroups, popularPosts);
    }

    private List<FindHomeRes.AnimalDTO> findRecommendedAnimals(Long userId) {
        List<Long> recommendedAnimalIds = animalService.findRecommendedAnimalIds(userId);

        return animalRepository.findByIds(recommendedAnimalIds).stream()
                .map(animal -> {
                    Long likeCount = likeService.getAnimalLikeCount(animal.getId());
                    return new FindHomeRes.AnimalDTO(animal, likeCount);
                })
                .toList();
    }

    private List<FindHomeRes.PostDTO> findPopularPosts() {
        Pageable pageable = PageRequest.of(POPULAR_POST_PAGE_INDEX, POPULAR_POST_PAGE_SIZE, Sort.by(Sort.Direction.DESC, SORT_BY_DATE));
        List<PopularPost> popularPosts = popularPostRepository.findAllWithPost(pageable).getContent();

        return popularPosts.stream()
                .map(PopularPost::getPost)
                .map(post -> {
                    String imageURL = extractFirstImageUrl(post);
                    Long likeCount = likeService.getPostLikeCount(post.getId());
                    return new FindHomeRes.PostDTO(post, likeCount, imageURL);
                })
                .toList();
    }

    private List<FindHomeRes.RecommendGroupDTO> findRecommendedGroups(Long userId) {
        List<Long> likedGroupIds = Optional.ofNullable(userId)
                .map(favoriteGroupRepository::findGroupIdByUserId)
                .orElse(Collections.emptyList());

        return groupService.findRecommendGroups(userId, DEFAULT_PROVINCE, likedGroupIds)
                .stream()
                .map(FindHomeRes.RecommendGroupDTO::new)
                .toList();
    }

    private String extractFirstImageUrl(Post post) {
        return post.getPostImages().isEmpty() ? null : post.getPostImages().get(0).getImageURL();
    }
}