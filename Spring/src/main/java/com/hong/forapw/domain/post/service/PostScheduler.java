package com.hong.forapw.domain.post.service;

import com.hong.forapw.domain.like.LikeService;
import com.hong.forapw.domain.like.common.Like;
import com.hong.forapw.domain.post.entity.PopularPost;
import com.hong.forapw.domain.post.entity.Post;
import com.hong.forapw.domain.post.constant.PostType;
import com.hong.forapw.domain.post.repository.CommentRepository;
import com.hong.forapw.domain.post.repository.PopularPostRepository;
import com.hong.forapw.domain.post.repository.PostRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.hong.forapw.domain.post.constant.PostConstants.*;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class PostScheduler {

    private final PostRepository postRepository;
    private final PopularPostRepository popularPostRepository;
    private final CommentRepository commentRepository;
    private final PostCacheService postCacheService;
    private final LikeService likeService;

    @Scheduled(cron = "0 0 6,9,12,15,18,21 * * *")
    public void updatePopularPosts() {
        for (PostType postType : POPULAR_POST_TYPES) {
            updatePopularPostsByType(postType);
        }
    }

    @Scheduled(cron = "0 0 0,3 * * *")
    public void updatePopularPostsOffHours() {
        for (PostType postType : POPULAR_POST_TYPES) {
            updatePopularPostsByType(postType);
        }
    }

    @Scheduled(cron = "0 25 0 * * *")
    public void syncViewNum() {
        LocalDateTime oneWeekAgo = LocalDateTime.now().minusWeeks(1);
        List<Post> posts = postRepository.findPostIdsWithinDate(oneWeekAgo);

        for (Post post : posts) {
            Long readCount = postCacheService.getPostViewCount(post);

            if (readCount != null) {
                post.updateReadCnt(readCount); // 조회수 갱신
            }
        }
    }

    private void updatePopularPostsByType(PostType postType) {
        LocalDateTime now = LocalDateTime.now();

        // 인기글 목록 초기화
        popularPostRepository.deleteByPostType(postType);

        // 단기(3일), 중기(7일), 장기(30일) 인기글 목록 조회
        List<Post> shortTermPopularPosts = findPopularPostsWithinRange(
                now.minusDays(SHORT_TERM_DAYS), now, postType, 0.6);

        List<Post> mediumTermPopularPosts = findPopularPostsWithinRange(
                now.minusDays(MEDIUM_TERM_DAYS), now, postType, 0.3);

        List<Post> longTermPopularPosts = findPopularPostsWithinRange(
                now.minusDays(LONG_TERM_DAYS), now, postType, 0.1);

        // 최종 인기글 목록 생성 (중복 제거)
        Map<Long, Post> finalPopularPosts = new LinkedHashMap<>();

        // 단기, 중기, 장기 순으로 추가 (중복 제거)
        shortTermPopularPosts.forEach(post -> finalPopularPosts.put(post.getId(), post));
        mediumTermPopularPosts.forEach(post -> finalPopularPosts.put(post.getId(), post));
        longTermPopularPosts.forEach(post -> finalPopularPosts.put(post.getId(), post));

        savePopularPosts(
                finalPopularPosts.values().stream()
                        .limit(POPULAR_POSTS_PER_TYPE)
                        .toList(),
                postType
        );
    }

    private List<Post> findPopularPostsWithinRange(LocalDateTime start, LocalDateTime end,
                                                   PostType postType, double timeWeightFactor) {
        List<Post> posts = postRepository.findByDateRangeAndType(start, end, postType);
        Map<Post, Double> postHotPoints = calculateHotPointsWithTimeDecay(posts, end, timeWeightFactor);

        // 인기도 높은 순으로 정렬
        return postHotPoints.entrySet().stream()
                .sorted(Map.Entry.<Post, Double>comparingByValue().reversed())
                .map(Map.Entry::getKey)
                .limit(POPULAR_POSTS_PER_TYPE)
                .toList();
    }

    private Map<Post, Double> calculateHotPointsWithTimeDecay(List<Post> posts, LocalDateTime currentTime,
                                                              double timeWeightFactor) {
        Map<Post, Double> hotPoints = new HashMap<>();

        for (Post post : posts) {
            // 시간 감소 계수 계산 (최신 글일수록 높은 가중치)
            long hoursAge = ChronoUnit.HOURS.between(post.getCreatedDate(), currentTime);
            double timeDecay = Math.max(0.2, 1.0 - (hoursAge / (24.0 * 30))); // 최소 0.2, 시간에 따라 감소

            // 기본 인기도 계산 (조회수, 댓글 수, 좋아요 수 기반)
            double viewPoints = postCacheService.getPostViewCount(post.getId(), post) * 0.002;
            double commentPoints = post.getCommentNum() * 1.5;
            double likePoints = likeService.getLikeCount(post.getId(), Like.POST) * 7.0;

            // 답변수 점수 (질문 게시글인 경우)
            double answerPoints = 0;
            if (post.isQuestionType()) {
                answerPoints = post.getAnswerNum() * 10.0; // 답변은 높은 가중치
            }

            // 활동 집중도 계산 (최근 활동이 집중되면 높은 점수)
            double activityDensity = calculateActivityDensity(post);

            // 최종 인기도 계산 (시간 가중치 적용)
            double hotPoint = (viewPoints + commentPoints + likePoints + answerPoints + activityDensity)
                    * timeDecay * timeWeightFactor;

            hotPoints.put(post, hotPoint);
            post.updateHotPoint(hotPoint);
        }

        return hotPoints;
    }

    private double calculateActivityDensity(Post post) {
        LocalDateTime oneDayAgo = LocalDateTime.now().minusDays(1);

        long recentComments = commentRepository.countByPostIdAndCreatedDateAfter(post.getId(), oneDayAgo);
        long recentLikes = likeService.getLikeCountAfter(post.getId(), Like.POST, oneDayAgo);

        double totalComments = post.getCommentNum();
        double totalLikes = likeService.getLikeCount(post.getId(), Like.POST);

        // 최근 활동 비율 계산 (최근 활동이 많을수록 높은 점수)
        double commentRatio = totalComments > 0 ? recentComments / totalComments : 0;
        double likeRatio = totalLikes > 0 ? recentLikes / totalLikes : 0;

        return (commentRatio + likeRatio) * 10.0; // 가중치 적용
    }

    private void savePopularPosts(List<Post> popularPosts, PostType postType) {
        popularPosts.forEach(post -> {
            PopularPost popularPost = PopularPost.builder()
                    .post(post)
                    .postType(postType)
                    .build();

            popularPostRepository.save(popularPost);
        });
    }
}
