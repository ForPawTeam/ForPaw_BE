package com.hong.forapw.domain.shelter.model.response;

import com.hong.forapw.domain.shelter.Shelter;

public record FindShelterInfoByIdRes(
        Long id,
        String name,
        Double lat,
        Double lng,
        String careAddr,
        String careTel,
        Long animalCnt
) {
    public static FindShelterInfoByIdRes fromEntity(Shelter shelter) {
        return new FindShelterInfoByIdRes(
                shelter.getId(),
                shelter.getName(),
                shelter.getLatitude(),
                shelter.getLongitude(),
                shelter.getCareAddr(),
                shelter.getCareTel(),
                shelter.getAnimalCnt()
        );
    }
}