package com.hong.forapw.domain.animal.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QAnimal is a Querydsl query type for Animal
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QAnimal extends EntityPathBase<Animal> {

    private static final long serialVersionUID = 219373713L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QAnimal animal = new QAnimal("animal");

    public final com.hong.forapw.common.entity.QBaseEntity _super = new com.hong.forapw.common.entity.QBaseEntity(this);

    public final StringPath age = createString("age");

    public final EnumPath<com.hong.forapw.domain.animal.constant.AnimalType> category = createEnum("category", com.hong.forapw.domain.animal.constant.AnimalType.class);

    public final StringPath color = createString("color");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdDate = _super.createdDate;

    public final StringPath gender = createString("gender");

    public final DatePath<java.time.LocalDate> happenDt = createDate("happenDt", java.time.LocalDate.class);

    public final StringPath happenPlace = createString("happenPlace");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final NumberPath<Long> inquiryNum = createNumber("inquiryNum", Long.class);

    public final StringPath introductionContent = createString("introductionContent");

    public final StringPath introductionTitle = createString("introductionTitle");

    public final BooleanPath isAdopted = createBoolean("isAdopted");

    public final StringPath kind = createString("kind");

    public final StringPath name = createString("name");

    public final StringPath neuter = createString("neuter");

    public final DatePath<java.time.LocalDate> noticeEdt = createDate("noticeEdt", java.time.LocalDate.class);

    public final DatePath<java.time.LocalDate> noticeSdt = createDate("noticeSdt", java.time.LocalDate.class);

    public final StringPath processState = createString("processState");

    public final StringPath profileURL = createString("profileURL");

    public final StringPath region = createString("region");

    public final DateTimePath<java.time.LocalDateTime> removedAt = createDateTime("removedAt", java.time.LocalDateTime.class);

    public final com.hong.forapw.domain.shelter.QShelter shelter;

    public final StringPath specialMark = createString("specialMark");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedDate = _super.updatedDate;

    public final StringPath weight = createString("weight");

    public QAnimal(String variable) {
        this(Animal.class, forVariable(variable), INITS);
    }

    public QAnimal(Path<? extends Animal> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QAnimal(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QAnimal(PathMetadata metadata, PathInits inits) {
        this(Animal.class, metadata, inits);
    }

    public QAnimal(Class<? extends Animal> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.shelter = inits.isInitialized("shelter") ? new com.hong.forapw.domain.shelter.QShelter(forProperty("shelter"), inits.get("shelter")) : null;
    }

}

