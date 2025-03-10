package com.hong.forapw.domain.animal.entity;

import com.hong.forapw.common.exceptions.CustomException;
import com.hong.forapw.common.exceptions.ExceptionCode;
import com.hong.forapw.domain.animal.constant.AnimalType;
import com.hong.forapw.common.entity.BaseEntity;
import com.hong.forapw.domain.shelter.Shelter;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;


import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "animal_tb")
@SQLDelete(sql = "UPDATE animal_tb SET removed_at = NOW() WHERE id=?")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class Animal extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shelter_id")
    private Shelter shelter;

    @Id
    private Long id;

    @Column
    private LocalDate happenDt;

    @Column
    private String happenPlace;

    @Column
    private String kind;

    @Column
    private AnimalType category;

    @Column
    private String color;

    @Column
    private String age;

    @Column
    private String weight;

    // 공고 시작일/종료일
    @Column
    private LocalDate noticeSdt;

    @Column
    private LocalDate noticeEdt;

    @Column
    private String profileURL;

    // 보호중 여부
    @Column
    private String processState;

    @Column
    private String gender;

    // 중성화 여부
    @Column
    private String neuter;

    @Column
    private String specialMark;

    @Column
    private String region;

    @Column
    private String name;

    @Column
    private String introductionTitle;

    @Column(columnDefinition = "TEXT")
    private String introductionContent;

    @Column
    private Long inquiryNum = 0L;

    @Column
    private boolean isAdopted;

    @Column(name = "removed_at")
    private LocalDateTime removedAt;

    @Builder
    public Animal(Long id, Shelter shelter, LocalDate happenDt, String happenPlace, String kind, AnimalType category, String color, String age, String weight, LocalDate noticeSdt, LocalDate noticeEdt, String profileURL, String processState, String gender, String neuter, String specialMark, String name, String region, String introductionContent, String introductionTitle, boolean isAdopted) {
        this.id = id;
        this.shelter = shelter;
        this.happenDt = happenDt;
        this.happenPlace = happenPlace;
        this.kind = kind;
        this.category = category;
        this.color = color;
        this.age = age;
        this.weight = weight;
        this.noticeSdt = noticeSdt;
        this.noticeEdt = noticeEdt;
        this.profileURL = profileURL;
        this.processState = processState;
        this.gender = gender;
        this.neuter = neuter;
        this.specialMark = specialMark;
        this.name = name;
        this.region = region;
        this.introductionContent = introductionContent;
        this.introductionTitle = introductionTitle;
        this.isAdopted = false;
    }

    public void incrementInquiryNum() {
        inquiryNum++;
    }

    public void decrementInquiryNum() {
        if(inquiryNum > 0) {
            inquiryNum--;
        }
    }

    public void finishAdoption() {
        isAdopted = true;
    }

    public void validateNotAdopted() {
        if (this.isAdopted()) {
            throw new CustomException(ExceptionCode.ANIMAL_ALREADY_ADOPTED);
        }
    }
}
