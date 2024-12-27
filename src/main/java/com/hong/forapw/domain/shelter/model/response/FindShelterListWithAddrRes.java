package com.hong.forapw.domain.shelter.model.response;

import com.hong.forapw.domain.region.constant.District;
import com.hong.forapw.domain.region.constant.Province;
import com.hong.forapw.domain.shelter.Shelter;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public record FindShelterListWithAddrRes(Map<String, List<DistrictShelterDTO>> province) {

    public static FindShelterListWithAddrRes fromShelters(List<Shelter> shelters) {
        Map<Province, Map<District, List<Shelter>>> sheltersGroupedByProvinceAndDistrict = groupSheltersByProvinceAndDistrict(shelters);

        Map<String, List<DistrictShelterDTO>> provinceShelterMap = sheltersGroupedByProvinceAndDistrict.entrySet().stream()
                .collect(Collectors.toMap(
                        provinceEntry -> provinceEntry.getKey().name(),
                        provinceEntry -> convertToDistrictDTOs(provinceEntry.getValue())
                ));
        return new FindShelterListWithAddrRes(provinceShelterMap);
    }

    private static Map<Province, Map<District, List<Shelter>>> groupSheltersByProvinceAndDistrict(List<Shelter> shelters) {
        return shelters.stream()
                .collect(Collectors.groupingBy(
                        Shelter::getUprName,
                        Collectors.groupingBy(Shelter::getOrgName)
                ));
    }

    private static List<DistrictShelterDTO> convertToDistrictDTOs(Map<District, List<Shelter>> districtShelters) {
        return districtShelters.entrySet().stream()
                .map(districtEntry -> {
                    Map<String, List<String>> shelterNamesByDistrict = Map.of(
                            districtEntry.getKey().name(),
                            districtEntry.getValue().stream()
                                    .map(Shelter::getName)
                                    .toList()
                    );
                    return new DistrictShelterDTO(shelterNamesByDistrict);
                })
                .toList();
    }

    public record DistrictShelterDTO(Map<String, List<String>> district) {
    }
}