package com.hong.forapw.domain.apply.model.response;

import com.hong.forapw.domain.apply.constant.ApplyStatus;
import com.hong.forapw.domain.apply.entity.Apply;

import java.util.List;

public record FindApplyListRes(List<ApplyDTO> applies) {

    public record ApplyDTO(
            Long applyId,
            Long animalId,
            String animalName,
            String kind,
            String gender,
            String age,
            String userName,
            String tel,
            String roadNameAddress,
            String addressDetail,
            String zipCode,
            ApplyStatus status
    ) {

        public ApplyDTO(Apply apply) {
            this(
                    apply.getId(),
                    apply.getAnimal().getId(),
                    apply.getAnimal().getName(),
                    apply.getAnimal().getKind(),
                    apply.getAnimal().getGender(),
                    apply.getAnimal().getAge(),
                    apply.getName(),
                    apply.getTel(),
                    apply.getRoadNameAddress(),
                    apply.getAddressDetail(),
                    apply.getZipCode(),
                    apply.getStatus()
            );
        }

        public static List<ApplyDTO> fromEntities(List<Apply> applies) {
            return applies.stream()
                    .map(FindApplyListRes.ApplyDTO::new)
                    .toList();
        }
    }
}