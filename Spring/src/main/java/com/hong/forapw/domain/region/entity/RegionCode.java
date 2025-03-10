package com.hong.forapw.domain.region.entity;

import com.hong.forapw.domain.region.constant.District;
import com.hong.forapw.domain.region.constant.Province;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "regionCode_tb")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class RegionCode {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    private Integer uprCd; // 시도 코드

    @Column
    private Integer orgCd; // 시군구 코드

    @Column
    @Enumerated(EnumType.STRING)
    private Province uprName; // 시도명

    @Column
    @Enumerated(EnumType.STRING)
    private District orgName;  // 시도군명

    @Builder
    public RegionCode(Integer uprCd, Integer orgCd, Province uprName, District orgName) {
        this.uprCd = uprCd;
        this.orgCd = orgCd;
        this.uprName = uprName;
        this.orgName = orgName;
    }
}
