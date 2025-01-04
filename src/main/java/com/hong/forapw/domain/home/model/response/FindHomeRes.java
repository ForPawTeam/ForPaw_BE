package com.hong.forapw.domain.home.model.response;

import com.hong.forapw.domain.animal.entity.Animal;
import com.hong.forapw.domain.group.model.response.RecommendGroupDTO;
import com.hong.forapw.domain.post.constant.PostType;
import com.hong.forapw.domain.post.entity.Post;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

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

        public AnimalDTO(Animal animal, Map<Long, Long> likeCountMap) {
            this(
                    animal.getId(),
                    animal.getName(),
                    animal.getAge(),
                    animal.getGender(),
                    animal.getSpecialMark(),
                    animal.getRegion(),
                    animal.getInquiryNum(),
                    likeCountMap.getOrDefault(animal.getId(), 0L),
                    animal.getProfileURL());
        }

        public static List<AnimalDTO> fromEntities(List<Animal> animals, Map<Long, Long> likeCountMap) {
            return animals.stream()
                    .map(animal -> new AnimalDTO(animal, likeCountMap))
                    .toList();
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

        public PostDTO(Post post, Map<Long, Long> likeCountMap) {
            this(
                    post.getId(),
                    post.getWriterNickName(),
                    post.getTitle(),
                    post.getContent(),
                    post.getCreatedDate(),
                    post.getCommentNum(),
                    likeCountMap.getOrDefault(post.getId(), 0L),
                    post.getPostImages().isEmpty() ? null : post.getPostImages().get(0).getImageURL(),
                    post.getPostType());
        }

        public static List<PostDTO> fromEntities(List<Post> posts, Map<Long, Long> likeCountMap) {
            return posts.stream()
                    .map(post -> new PostDTO(post, likeCountMap))
                    .toList();
        }
    }
}
