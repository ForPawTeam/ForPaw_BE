package com.hong.forapw.domain.search.model.response;

import com.hong.forapw.domain.shelter.Shelter;

import java.util.List;

public record ShelterDTO(
        Long id,
        String name
) {

    public static List<ShelterDTO> fromEntities(List<Shelter> shelters) {
        return shelters.stream()
                .map(shelter -> new ShelterDTO(shelter.getId(), shelter.getName()))
                .toList();
    }
}