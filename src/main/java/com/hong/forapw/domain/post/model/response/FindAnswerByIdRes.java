package com.hong.forapw.domain.post.model.response;

import com.hong.forapw.domain.post.entity.Post;

import java.time.LocalDateTime;
import java.util.List;

public record FindAnswerByIdRes(
        String nickName,
        String content,
        LocalDateTime date,
        List<PostImageDTO> images,
        boolean isMine
) {
    public FindAnswerByIdRes(Post answer, Long userId) {
        this(
                answer.getWriterNickName(),
                answer.getContent(),
                answer.getCreatedDate(),
                PostImageDTO.fromEntity(answer),
                answer.isOwner(userId)
        );
    }

    public record PostImageDTO(
            Long id,
            String imageURL) {

        public static List<PostImageDTO> fromEntity(Post post) {
            return post.getPostImages().stream()
                    .map(postImage -> new PostImageDTO(postImage.getId(), postImage.getImageURL()))
                    .toList();
        }
    }
}