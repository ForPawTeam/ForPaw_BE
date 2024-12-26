package com.hong.forapw.domain.post.model.response;

import com.hong.forapw.domain.post.entity.Comment;
import com.hong.forapw.domain.post.entity.Post;
import com.hong.forapw.domain.region.constant.Province;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public record FindPostByIdRes(
        String nickName,
        String profileURL,
        String title,
        String content,
        LocalDateTime date,
        Long commentNum,
        Long likeNum,
        boolean isMine,
        boolean isLike,
        List<PostImageDTO> images,
        List<CommentDTO> comments
) {
    public FindPostByIdRes(Post post, List<FindPostByIdRes.CommentDTO> commentDTOs, Long userId, Long likeCount, boolean isPostLiked) {
        this(
                post.getUser().getNickname(),
                post.getUser().getProfileURL(),
                post.getTitle(),
                post.getContent(),
                post.getCreatedDate(),
                post.getCommentNum(),
                likeCount,
                post.isOwner(userId),
                isPostLiked,
                PostImageDTO.fromEntity(post),
                commentDTOs
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

    public record CommentDTO(
            Long id,
            String nickName,
            String profileURL,
            String content,
            LocalDateTime date,
            Province location,
            Long likeNum,
            boolean isLike,
            List<ReplyDTO> replies) {

        public static CommentDTO fromEntity(Comment comment, Long likeCount, boolean isLikedByUser) {
            return new CommentDTO(
                    comment.getId(),
                    comment.getWriterNickname(),
                    comment.getWriterProfileURL(),
                    comment.getContent(),
                    comment.getCreatedDate(),
                    comment.getWriterProvince(),
                    likeCount,
                    isLikedByUser,
                    new ArrayList<>() // 답변을 담을 리스트
            );
        }
    }

    public record ReplyDTO(
            Long id,
            String nickName,
            String profileURL,
            String replyName,
            String content,
            LocalDateTime date,
            Province location,
            Long likeNum,
            boolean isLike) {

        public static ReplyDTO fromEntity(Comment childComment, boolean isLikedByUser, Long likeCount) {
            return new ReplyDTO(
                    childComment.getId(),
                    childComment.getWriterNickname(),
                    childComment.getWriterProfileURL(),
                    childComment.getParentWriterNickname(),
                    childComment.getContent(),
                    childComment.getCreatedDate(),
                    childComment.getWriterProvince(),
                    likeCount,
                    isLikedByUser
            );
        }
    }
}