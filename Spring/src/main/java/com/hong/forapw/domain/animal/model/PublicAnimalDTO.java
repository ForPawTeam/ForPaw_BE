package com.hong.forapw.domain.animal.model;

import com.hong.forapw.domain.animal.constant.AnimalType;
import com.hong.forapw.domain.animal.entity.Animal;
import com.hong.forapw.domain.shelter.Shelter;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static com.hong.forapw.common.utils.DateTimeUtils.YEAR_HOUR_DAY_FORMAT;
import static com.hong.forapw.common.utils.UriUtils.convertToHttpsUri;

public record PublicAnimalDTO(ResponseDTO response) {
    public record ResponseDTO(HeaderDTO header, BodyDTO body) {
    }

    public record HeaderDTO(Long reqNo, String resultCode, String resultMsg) {
    }

    public record BodyDTO(ItemsDTO items,
                          Integer numOfRows,
                          Integer pageNo,
                          Integer totalCount) {
    }

    public record ItemsDTO(List<AnimalDTO> item) {
    }

    public record AnimalDTO(String desertionNo,
                            String filename,
                            String happenDt,
                            String happenPlace,
                            String kindCd,
                            String colorCd,
                            String age,
                            String weight,
                            String noticeNo,
                            String noticeSdt,
                            String noticeEdt,
                            String popfile,
                            String processState,
                            String sexCd,
                            String neuterYn,
                            String specialMark,
                            String careNm,
                            String careTel,
                            String careAddr,
                            String orgNm,
                            String chargeNm,
                            String officetel
    ) {
        public Animal toEntity(String name, String kind, Shelter shelter) {
            DateTimeFormatter formatter = YEAR_HOUR_DAY_FORMAT;
            return Animal.builder()
                    .id(Long.valueOf(desertionNo))
                    .name(name)
                    .shelter(shelter)
                    .happenDt(LocalDate.parse(happenDt, formatter))
                    .happenPlace(happenPlace)
                    .kind(kind)
                    .category(AnimalType.fromPrefix(kindCd))
                    .color(colorCd)
                    .age(age)
                    .weight(weight)
                    .noticeSdt(LocalDate.parse(noticeSdt, formatter))
                    .noticeEdt(LocalDate.parse(noticeEdt, formatter))
                    .profileURL(convertToHttpsUri(popfile))
                    .processState(processState)
                    .gender(sexCd)
                    .neuter(neuterYn)
                    .specialMark(specialMark)
                    .region(shelter.getRegionCode().getUprName() + " " + shelter.getRegionCode().getOrgName())
                    .introductionContent("소개글을 작성중입니다!")
                    .build();
        }
    }
}