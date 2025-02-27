package com.hong.forapw.domain.post.model.response;

import com.hong.forapw.domain.post.entity.Post;

import java.time.LocalDateTime;
import java.util.List;

public record FindQnaByIdRes(
        String nickName,
        String profileURL,
        String title,
        String content,
        LocalDateTime date,
        List<PostImageDTO> images,
        List<AnswerDTO> answers,
        boolean isMine) {

    public FindQnaByIdRes(Post qna, List<Post> answers, Long userId) {
        this(
                qna.getWriterNickName(),
                qna.getWriterProfileURL(),
                qna.getTitle(),
                qna.getContent(),
                qna.getCreatedDate(),
                PostImageDTO.fromEntity(qna),
                AnswerDTO.fromEntity(answers, userId),
                qna.isOwner(userId)
        );
    }

    public record AnswerDTO(
            Long id,
            String nickName,
            String profileURL,
            String content,
            LocalDateTime date,
            List<PostImageDTO> images,
            boolean isMine) {

        public static List<AnswerDTO> fromEntity(List<Post> answers, Long userId) {
            return answers.stream()
                    .map(answer -> new AnswerDTO(
                            answer.getId(),
                            answer.getWriterNickName(),
                            answer.getWriterProfileURL(),
                            answer.getContent(),
                            answer.getCreatedDate(),
                            PostImageDTO.fromEntity(answer),
                            answer.isOwner(userId)))
                    .toList();
        }
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
