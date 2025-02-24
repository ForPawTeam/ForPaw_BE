package com.hong.forapw.domain.region.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum Province {

    SEOUL("서울특별시"),
    DAEGU("대구광역시"),
    BUSAN("부산광역시"),
    INCHEON("인천광역시"),
    GWANGJU("광주광역시"),
    DAEJEON("대전광역시"),
    ULSAN("울산광역시"),
    GYEONGGI("경기도"),
    GANGWON("강원특별자치도"),
    CHUNGCHEONGNORTH("충청북도"),
    CHUNGCHEONGSOUTH("충청남도"),
    JEONLANORTH("전라북도"),
    JEONLASOUTH("전라남도"),
    GYEONGSANGNORTH("경상북도"),
    GYEONGSANGSOUTH("경상남도"),
    JEJU("제주특별자치도");

    private final String value;
}