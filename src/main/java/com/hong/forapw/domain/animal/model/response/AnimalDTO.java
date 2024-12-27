package com.hong.forapw.domain.animal.model.response;

import com.hong.forapw.domain.animal.entity.Animal;

import java.util.List;

public record AnimalDTO(
        Long id,
        String name,
        String age,
        String gender,
        String specialMark,
        String kind,
        String weight,
        String neuter,
        String processState,
        String region,
        Long inquiryNum,
        Long likeNum,
        Boolean isLike,
        String profileURL
) {
    public static AnimalDTO fromEntity(Animal animal, Long likeCount, List<Long> likedAnimalIds) {
        return new AnimalDTO(
                animal.getId(),
                animal.getName(),
                animal.getAge(),
                animal.getGender(),
                animal.getSpecialMark(),
                animal.getKind(),
                animal.getWeight(),
                animal.getNeuter(),
                animal.getProcessState(),
                animal.getRegion(),
                animal.getInquiryNum(),
                likeCount,
                likedAnimalIds.contains(animal.getId()),
                animal.getProfileURL());
    }
}