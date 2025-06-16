package com.hong.forapw.domain.group.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QGroup is a Querydsl query type for Group
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QGroup extends EntityPathBase<Group> {

    private static final long serialVersionUID = 935961887L;

    public static final QGroup group = new QGroup("group1");

    public final com.hong.forapw.common.entity.QBaseEntity _super = new com.hong.forapw.common.entity.QBaseEntity(this);

    public final StringPath category = createString("category");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdDate = _super.createdDate;

    public final StringPath description = createString("description");

    public final EnumPath<com.hong.forapw.domain.region.constant.District> district = createEnum("district", com.hong.forapw.domain.region.constant.District.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final BooleanPath isShelterOwns = createBoolean("isShelterOwns");

    public final NumberPath<Long> maxNum = createNumber("maxNum", Long.class);

    public final StringPath name = createString("name");

    public final NumberPath<Long> participantNum = createNumber("participantNum", Long.class);

    public final StringPath profileURL = createString("profileURL");

    public final EnumPath<com.hong.forapw.domain.region.constant.Province> province = createEnum("province", com.hong.forapw.domain.region.constant.Province.class);

    public final StringPath shelterName = createString("shelterName");

    public final StringPath subDistrict = createString("subDistrict");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedDate = _super.updatedDate;

    public QGroup(String variable) {
        super(Group.class, forVariable(variable));
    }

    public QGroup(Path<? extends Group> path) {
        super(path.getType(), path.getMetadata());
    }

    public QGroup(PathMetadata metadata) {
        super(Group.class, metadata);
    }

}

