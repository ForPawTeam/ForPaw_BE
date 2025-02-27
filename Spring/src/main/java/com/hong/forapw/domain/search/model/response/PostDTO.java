package com.hong.forapw.domain.search.model.response;

import com.hong.forapw.domain.post.constant.PostType;
import com.hong.forapw.domain.post.model.query.PostProjection;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record PostDTO(
        Long id,
        PostType type,
        String title,
        String content,
        LocalDateTime date,
        String imageURL,
        String nickName,
        Long commentNum,
        Long likeNum
) {

    public PostDTO(PostProjection queryResult, Map<Long, Long> likeCountMap) {
        this(
                queryResult.getPostId(),
                PostType.valueOf(queryResult.getPostType().toUpperCase()),
                queryResult.getTitle(),
                queryResult.getContent(),
                queryResult.getCreatedDate(),
                queryResult.getImageUrl(),
                queryResult.getNickName(),
                queryResult.getCommentNum(),
                likeCountMap.getOrDefault(queryResult.getPostId(), 0L)
        );
    }

    public static List<PostDTO> fromQureryResults(List<PostProjection> queryResults, Map<Long, Long> likeCountMap) {
        return queryResults.stream()
                .map(projection -> new PostDTO(projection, likeCountMap))
                .toList();
    }
}