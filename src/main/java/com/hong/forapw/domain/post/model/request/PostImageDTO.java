package com.hong.forapw.domain.post.model.request;

import com.hong.forapw.domain.post.entity.PostImage;

import java.util.List;

public record PostImageDTO(String imageURL) {

    public static List<PostImage> fromDTOs(List<PostImageDTO> imageDTOs) {
        return imageDTOs.stream()
                .map(dto -> PostImage.builder()
                        .imageURL(dto.imageURL())
                        .build())
                .toList();
    }
}