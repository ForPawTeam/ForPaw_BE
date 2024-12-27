package com.hong.forapw.domain.shelter.model;


import com.hong.forapw.domain.region.entity.RegionCode;
import com.hong.forapw.domain.shelter.Shelter;

import java.util.List;

public record PublicShelterDTO(ResponseDTO response) {

    public record ResponseDTO(HeaderDTO header, BodyDTO body) {
    }

    public record HeaderDTO(Long reqNo,
                            String resultCode,
                            String resultMsg,
                            String errorMsg) {
    }

    public record BodyDTO(ItemsDTO items) {
    }

    public record ItemsDTO(List<itemDTO> item) {
    }

    public record itemDTO(String careNm, Long careRegNo
    ) {
        public Shelter toEntity(RegionCode regionCode) {
            return Shelter.builder()
                    .regionCode(regionCode)
                    .id(careRegNo)
                    .name(careNm)
                    .build();
        }
    }
}