package com.hong.forapw.domain.user.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QUser is a Querydsl query type for User
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QUser extends EntityPathBase<User> {

    private static final long serialVersionUID = -94458385L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QUser user = new QUser("user");

    public final com.hong.forapw.common.entity.QBaseEntity _super = new com.hong.forapw.common.entity.QBaseEntity(this);

    public final EnumPath<com.hong.forapw.domain.user.constant.AuthProvider> authProvider = createEnum("authProvider", com.hong.forapw.domain.user.constant.AuthProvider.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdDate = _super.createdDate;

    public final EnumPath<com.hong.forapw.domain.region.constant.District> district = createEnum("district", com.hong.forapw.domain.region.constant.District.class);

    public final StringPath email = createString("email");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final BooleanPath isMarketingAgreed = createBoolean("isMarketingAgreed");

    public final StringPath name = createString("name");

    public final StringPath nickname = createString("nickname");

    public final StringPath password = createString("password");

    public final StringPath profileURL = createString("profileURL");

    public final EnumPath<com.hong.forapw.domain.region.constant.Province> province = createEnum("province", com.hong.forapw.domain.region.constant.Province.class);

    public final DateTimePath<java.time.LocalDateTime> removedAt = createDateTime("removedAt", java.time.LocalDateTime.class);

    public final EnumPath<com.hong.forapw.domain.user.constant.UserRole> role = createEnum("role", com.hong.forapw.domain.user.constant.UserRole.class);

    public final QUserStatus status;

    public final StringPath subDistrict = createString("subDistrict");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedDate = _super.updatedDate;

    public QUser(String variable) {
        this(User.class, forVariable(variable), INITS);
    }

    public QUser(Path<? extends User> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QUser(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QUser(PathMetadata metadata, PathInits inits) {
        this(User.class, metadata, inits);
    }

    public QUser(Class<? extends User> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.status = inits.isInitialized("status") ? new QUserStatus(forProperty("status"), inits.get("status")) : null;
    }

}

