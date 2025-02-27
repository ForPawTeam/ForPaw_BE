package com.hong.forapw.domain.post.model.response;

import com.hong.forapw.domain.post.entity.Post;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record FindPostListRes(
        List<PostDTO> posts,
        boolean isLastPage) {

    public record PostDTO(
            Long id,
            String nickName,
            String title,
            String content,
            LocalDateTime date,
            Long commentNum,
            Long likeNum,
            String imageURL,
            boolean isBlocked
    ) {

        public PostDTO(Post post, Map<Long, Long> likeCountMap) {
            this(
                    post.getId(),
                    post.getWriterNickName(),
                    post.getTitle(),
                    post.getContent(),
                    post.getCreatedDate(),
                    post.getCommentNum(),
                    likeCountMap.getOrDefault(post.getId(), 0L),
                    post.getFirstImageURL(),
                    post.isBlocked()
            );
        }

        public static List<PostDTO> fromEntities(List<Post> posts, Map<Long, Long> likeCountMap) {
            return posts.stream()
                    .map(post -> new FindPostListRes.PostDTO(post, likeCountMap))
                    .toList();
        }
    }
}