package com.hong.ForPaw.domain;

import com.hong.ForPaw.domain.RegionCode;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class Shelter {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "regionCode_id")
    private RegionCode regionCode;

    // 보호소 등록 번호
    @Id
    private Long careRegNo;

    @Column
    private String name;

    @Column
    private String careTel;

    @Column
    private String careAddr;

    @Builder
    public Shelter(Long careRegNo, RegionCode regionCode, String name, String careTel, String careAddr) {
        this.careRegNo = careRegNo;
        this.regionCode = regionCode;
        this.name = name;
        this.careTel = careTel;
        this.careAddr = careAddr;
    }

    public void updateShelterInfo(String careTel, String careAddr){
        this.careTel = careTel;
        this.careAddr = careAddr;
    }
}