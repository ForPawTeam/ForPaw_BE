package com.hong.forapw.domain.post.model.response;

import com.hong.forapw.domain.post.entity.Post;
import org.springframework.data.domain.Page;

import java.time.LocalDateTime;
import java.util.List;

public record FindQnaListRes(
        List<QnaDTO> questions,
        boolean isLastPage
) {
    public FindQnaListRes(Page<Post> questionPage) {
        this(
                questionPage.getContent().stream()
                        .distinct() // 중복 제거
                        .map(QnaDTO::fromEntity)
                        .toList(),
                questionPage.isLast()
        );
    }

    public record QnaDTO(
            Long id,
            String nickName,
            String profileURL,
            String title,
            String content,
            LocalDateTime date,
            Long answerNum,
            boolean isBlocked) {

        public static QnaDTO fromEntity(Post post) {
            return new QnaDTO(
                    post.getId(),
                    post.getWriterNickName(),
                    post.getWriterProfileURL(),
                    post.getTitle(),
                    post.getContent(),
                    post.getCreatedDate(),
                    post.getAnswerNum(),
                    post.isBlocked()
            );
        }
    }
}