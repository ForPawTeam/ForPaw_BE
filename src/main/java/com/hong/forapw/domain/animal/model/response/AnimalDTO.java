package com.hong.forapw.domain.animal.model.response;

import com.hong.forapw.domain.animal.entity.Animal;

import java.util.Collections;
import java.util.List;
import java.util.Map;

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

    public AnimalDTO(Animal animal, Map<Long, Long> likeCountMap, List<Long> likedAnimalIds) {
        this(
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
                likeCountMap.getOrDefault(animal.getId(), 0L),
                likedAnimalIds.contains(animal.getId()),
                animal.getProfileURL());
    }

    public static List<AnimalDTO> fromEntities(List<Animal> animals, Map<Long, Long> likeCountMap, List<Long> likedAnimalIds) {
        return animals.stream()
                .map(animal -> new AnimalDTO(animal, likeCountMap, likedAnimalIds))
                .toList();
    }
}