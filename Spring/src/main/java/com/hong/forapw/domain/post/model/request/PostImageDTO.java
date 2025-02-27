package com.hong.forapw.domain.post.model.request;

import com.hong.forapw.domain.post.entity.Post;
import com.hong.forapw.domain.post.entity.PostImage;

import java.util.List;

public record PostImageDTO(String imageURL) {

    public static List<PostImage> fromDTOs(List<PostImageDTO> imageDTOs) {
        return imageDTOs.stream()
                .map(postImageDTO -> PostImage.builder()
                        .imageURL(postImageDTO.imageURL())
                        .build())
                .toList();
    }

    public static List<PostImage> fromDTDs(List<PostImageDTO> imageDTOs, Post post) {
        return imageDTOs.stream()
                .map(request -> PostImage.builder()
                        .post(post)
                        .imageURL(request.imageURL())
                        .build())
                .toList();
    }
}