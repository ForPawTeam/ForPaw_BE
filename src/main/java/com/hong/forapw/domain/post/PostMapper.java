package com.hong.forapw.domain.post;

import com.hong.forapw.domain.post.model.request.PostImageDTO;
import com.hong.forapw.domain.post.entity.Comment;
import com.hong.forapw.domain.post.entity.Post;
import com.hong.forapw.domain.post.entity.PostImage;
import com.hong.forapw.domain.post.model.response.*;

import java.util.ArrayList;
import java.util.List;

public class PostMapper {

    private PostMapper() {
    }

    public static List<PostImage> toPostImages(List<PostImageDTO> imageDTOs) {
        return imageDTOs.stream()
                .map(postImageDTO -> PostImage.builder()
                        .imageURL(postImageDTO.imageURL())
                        .build())
                .toList();
    }

    public static FindPostListRes.PostDTO toPostDTO(Post post, Long cachedPostLikeNum) {
        return new FindPostListRes.PostDTO(
                post.getId(),
                post.getWriterNickName(),
                post.getTitle(),
                post.getContent(),
                post.getCreatedDate(),
                post.getCommentNum(),
                cachedPostLikeNum,
                post.getFirstImageURL(),
                post.isBlocked()
        );
    }

    public static FindMyPostListRes.MyPostDTO toMyPostDTO(Post post, Long cachedPostLikeNum) {
        return new FindMyPostListRes.MyPostDTO(
                post.getId(),
                post.getWriterNickName(),
                post.getTitle(),
                post.getContent(),
                post.getCreatedDate(),
                post.getCommentNum(),
                cachedPostLikeNum,
                post.getFirstImageURL(),
                post.isBlocked(),
                post.getPostTypeString()
        );
    }

    public static FindPostByIdRes.CommentDTO toParentCommentDTO(Comment comment, Long likeCount, boolean isLikedByUser) {
        return new FindPostByIdRes.CommentDTO(
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
