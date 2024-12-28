package com.hong.forapw.domain.apply;

import com.hong.forapw.domain.apply.model.ApplyResponse;
import com.hong.forapw.domain.apply.entity.Apply;

public class ApplyMapper {

    private ApplyMapper() {
    }



    public static ApplyResponse.ApplyDTO toApplyDTO(Apply apply) {
        return new ApplyResponse.ApplyDTO(
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
                apply.getStatus());
    }
}
