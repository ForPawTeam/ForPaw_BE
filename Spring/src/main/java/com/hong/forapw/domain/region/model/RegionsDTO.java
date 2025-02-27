package com.hong.forapw.domain.region.model;

import com.hong.forapw.domain.region.constant.District;
import com.hong.forapw.domain.region.constant.Province;
import com.hong.forapw.domain.region.entity.RegionCode;

import java.util.List;

public record RegionsDTO(List<Region> regions) {

    public record Region(
            Integer orgCd,
            Province orgdownNm,
            List<SubRegion> subRegions) {
    }

    public record SubRegion(
            Integer uprCd,
            Integer orgCd,
            District orgdownNm
    ) {

        public RegionCode toEntity(Region region){
            return RegionCode.builder()
                    .uprCd(region.orgCd())
                    .uprName(region.orgdownNm())
                    .orgCd(orgCd)
                    .orgName(orgdownNm)
                    .build();
        }
    }

    public List<RegionCode> toEntities() {
        return this.regions().stream()
                .flatMap(region -> region.subRegions().stream()
                        .map(subRegion -> subRegion.toEntity(region))
                )
                .toList();
    }
}
