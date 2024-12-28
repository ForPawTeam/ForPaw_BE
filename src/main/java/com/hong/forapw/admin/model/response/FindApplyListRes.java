package com.hong.forapw.admin.model.response;

import com.hong.forapw.domain.animal.entity.Animal;
import com.hong.forapw.domain.apply.constant.ApplyStatus;
import com.hong.forapw.domain.apply.entity.Apply;
import org.springframework.data.domain.Page;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record FindApplyListRes(List<ApplyDTO> applies, int totalPages) {

    public record ApplyDTO(
            Long applyId,
            LocalDateTime applyDate,
            Long animalId,
            String kind,
            String gender,
            String age,
            String userName,
            String tel,
            String residence,
            String careName,
            String careTel,
            ApplyStatus status
    ) {
        public static List<FindApplyListRes.ApplyDTO> fromEntities(List<Apply> applies) {
            return applies.stream()
                    .map(apply -> new FindApplyListRes.ApplyDTO(
                            apply.getId(),
                            apply.getCreatedDate(),
                            apply.getAnimal().getId(),
                            apply.getAnimal().getKind(),
                            apply.getAnimal().getGender(),
                            apply.getAnimal().getAge(),
                            apply.getName(),
                            apply.getTel(),
                            apply.getAddressDetail(),
                            apply.getAnimal().getShelter().getName(),
                            apply.getAnimal().getShelter().getCareTel(),
                            apply.getStatus()))
                    .toList();
        }
    }
}