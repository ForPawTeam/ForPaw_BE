package com.hong.forapw.domain.region.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QRegionCode is a Querydsl query type for RegionCode
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QRegionCode extends EntityPathBase<RegionCode> {

    private static final long serialVersionUID = 985102862L;

    public static final QRegionCode regionCode = new QRegionCode("regionCode");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final NumberPath<Integer> orgCd = createNumber("orgCd", Integer.class);

    public final EnumPath<com.hong.forapw.domain.region.constant.District> orgName = createEnum("orgName", com.hong.forapw.domain.region.constant.District.class);

    public final NumberPath<Integer> uprCd = createNumber("uprCd", Integer.class);

    public final EnumPath<com.hong.forapw.domain.region.constant.Province> uprName = createEnum("uprName", com.hong.forapw.domain.region.constant.Province.class);

    public QRegionCode(String variable) {
        super(RegionCode.class, forVariable(variable));
    }

    public QRegionCode(Path<? extends RegionCode> path) {
        super(path.getType(), path.getMetadata());
    }

    public QRegionCode(PathMetadata metadata) {
        super(RegionCode.class, metadata);
    }

}

