package com.hong.forapw.domain.animal.model.response;

import com.hong.forapw.domain.animal.entity.Animal;

import java.time.LocalDate;

public record FindAnimalByIdRes(
        Long id,
        String name,
        String age,
        String gender,
        String specialMark,
        String region,
        Boolean isLike,
        String profileURL,
        String happenPlace,
        String kind,
        String color,
        String weight,
        LocalDate noticeSdt,
        LocalDate noticeEdt,
        String processState,
        String neuter,
        String introductionTitle,
        String introductionContent,
        boolean isAdopted
) {
    public FindAnimalByIdRes(Animal animal, boolean isLikedAnimal) {
        this(
                animal.getId(),
                animal.getName(),
                animal.getAge(),
                animal.getGender(),
                animal.getSpecialMark(),
                animal.getRegion(),
                isLikedAnimal,
                animal.getProfileURL(),
                animal.getHappenPlace(),
                animal.getKind(),
                animal.getColor(),
                animal.getWeight(),
                animal.getNoticeSdt(),
                animal.getNoticeEdt(),
                animal.getProcessState(),
                animal.getNeuter(),
                animal.getIntroductionTitle(),
                animal.getIntroductionContent(),
                animal.isAdopted()
        );
    }
}