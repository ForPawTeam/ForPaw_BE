package com.hong.forapw.domain.shelter;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QShelter is a Querydsl query type for Shelter
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QShelter extends EntityPathBase<Shelter> {

    private static final long serialVersionUID = 1055306944L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QShelter shelter = new QShelter("shelter");

    public final NumberPath<Long> animalCnt = createNumber("animalCnt", Long.class);

    public final StringPath careAddr = createString("careAddr");

    public final StringPath careTel = createString("careTel");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final BooleanPath isDuplicate = createBoolean("isDuplicate");

    public final NumberPath<Double> latitude = createNumber("latitude", Double.class);

    public final NumberPath<Double> longitude = createNumber("longitude", Double.class);

    public final StringPath name = createString("name");

    public final com.hong.forapw.domain.region.entity.QRegionCode regionCode;

    public QShelter(String variable) {
        this(Shelter.class, forVariable(variable), INITS);
    }

    public QShelter(Path<? extends Shelter> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QShelter(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QShelter(PathMetadata metadata, PathInits inits) {
        this(Shelter.class, metadata, inits);
    }

    public QShelter(Class<? extends Shelter> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.regionCode = inits.isInitialized("regionCode") ? new com.hong.forapw.domain.region.entity.QRegionCode(forProperty("regionCode")) : null;
    }

}

