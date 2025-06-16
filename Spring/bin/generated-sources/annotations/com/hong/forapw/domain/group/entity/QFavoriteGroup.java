package com.hong.forapw.domain.group.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QFavoriteGroup is a Querydsl query type for FavoriteGroup
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QFavoriteGroup extends EntityPathBase<FavoriteGroup> {

    private static final long serialVersionUID = -1031410461L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QFavoriteGroup favoriteGroup = new QFavoriteGroup("favoriteGroup");

    public final com.hong.forapw.common.entity.QBaseEntity _super = new com.hong.forapw.common.entity.QBaseEntity(this);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdDate = _super.createdDate;

    public final QGroup group;

    public final NumberPath<Long> id = createNumber("id", Long.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedDate = _super.updatedDate;

    public final com.hong.forapw.domain.user.entity.QUser user;

    public QFavoriteGroup(String variable) {
        this(FavoriteGroup.class, forVariable(variable), INITS);
    }

    public QFavoriteGroup(Path<? extends FavoriteGroup> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QFavoriteGroup(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QFavoriteGroup(PathMetadata metadata, PathInits inits) {
        this(FavoriteGroup.class, metadata, inits);
    }

    public QFavoriteGroup(Class<? extends FavoriteGroup> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.group = inits.isInitialized("group") ? new QGroup(forProperty("group")) : null;
        this.user = inits.isInitialized("user") ? new com.hong.forapw.domain.user.entity.QUser(forProperty("user"), inits.get("user")) : null;
    }

}

