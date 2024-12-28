package com.hong.forapw.domain.home.model.response;

import com.hong.forapw.domain.animal.entity.Animal;
import com.hong.forapw.domain.group.model.response.RecommendGroupDTO;
import com.hong.forapw.domain.post.constant.PostType;
import com.hong.forapw.domain.post.entity.Post;
import com.hong.forapw.domain.region.constant.District;
import com.hong.forapw.domain.region.constant.Province;

import java.time.LocalDateTime;
import java.util.List;

public record FindHomeRes(
        List<AnimalDTO> animals,
        List<RecommendGroupDTO> groups,
        List<PostDTO> posts) {

    public record AnimalDTO(
            Long id,
            String name,
            String age,
            String gender,
            String specialMark,
            String region,
            Long inquiryNum,
            Long likeNum,
            String profileURL
    ) {
        public AnimalDTO(Animal animal, Long likeNum) {
            this(
                    animal.getId(),
                    animal.getName(),
                    animal.getAge(),
                    animal.getGender(),
                    animal.getSpecialMark(),
                    animal.getRegion(),
                    animal.getInquiryNum(),
                    likeNum,
                    animal.getProfileURL());
        }
    }

    public record PostDTO(
            Long id,
            String name,
            String title,
            String content,
            LocalDateTime date,
            Long commentNum,
            Long likeNum,
            String imageURL,
            PostType postType
    ) {
        public PostDTO(Post post, Long likeNum, String imageURL) {
            this(
                    post.getId(),
                    post.getWriterNickName(),
                    post.getTitle(),
                    post.getContent(),
                    post.getCreatedDate(),
                    post.getCommentNum(),
                    likeNum,
                    imageURL,
                    post.getPostType());
        }
    }
}
