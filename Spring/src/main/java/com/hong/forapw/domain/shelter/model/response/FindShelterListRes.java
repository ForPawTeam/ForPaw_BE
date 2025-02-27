package com.hong.forapw.domain.shelter.model.response;

import com.hong.forapw.domain.shelter.Shelter;

import java.util.List;

public record FindShelterListRes(List<ShelterDTO> shelters) {

    public record ShelterDTO(
            Long id,
            String name,
            Double lat,
            Double lng,
            String careAddr,
            String careTel
    ) {
        public static ShelterDTO fromEntity(Shelter shelter) {
            return new ShelterDTO(
                    shelter.getId(),
                    shelter.getName(),
                    shelter.getLatitude(),
                    shelter.getLongitude(),
                    shelter.getCareAddr(),
                    shelter.getCareTel());
        }
    }
}