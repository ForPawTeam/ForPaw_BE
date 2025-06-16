package com.hong.forapw.domain.animal.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QFavoriteAnimal is a Querydsl query type for FavoriteAnimal
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QFavoriteAnimal extends EntityPathBase<FavoriteAnimal> {

    private static final long serialVersionUID = -1425142707L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QFavoriteAnimal favoriteAnimal = new QFavoriteAnimal("favoriteAnimal");

    public final com.hong.forapw.common.entity.QBaseEntity _super = new com.hong.forapw.common.entity.QBaseEntity(this);

    public final QAnimal animal;

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdDate = _super.createdDate;

    public final NumberPath<Long> id = createNumber("id", Long.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedDate = _super.updatedDate;

    public final com.hong.forapw.domain.user.entity.QUser user;

    public QFavoriteAnimal(String variable) {
        this(FavoriteAnimal.class, forVariable(variable), INITS);
    }

    public QFavoriteAnimal(Path<? extends FavoriteAnimal> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QFavoriteAnimal(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QFavoriteAnimal(PathMetadata metadata, PathInits inits) {
        this(FavoriteAnimal.class, metadata, inits);
    }

    public QFavoriteAnimal(Class<? extends FavoriteAnimal> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.animal = inits.isInitialized("animal") ? new QAnimal(forProperty("animal"), inits.get("animal")) : null;
        this.user = inits.isInitialized("user") ? new com.hong.forapw.domain.user.entity.QUser(forProperty("user"), inits.get("user")) : null;
    }

}

