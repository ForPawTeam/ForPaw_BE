package com.hong.forapw.admin.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QLoginAttempt is a Querydsl query type for LoginAttempt
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QLoginAttempt extends EntityPathBase<LoginAttempt> {

    private static final long serialVersionUID = -983417246L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QLoginAttempt loginAttempt = new QLoginAttempt("loginAttempt");

    public final com.hong.forapw.common.entity.QBaseEntity _super = new com.hong.forapw.common.entity.QBaseEntity(this);

    public final StringPath clientIp = createString("clientIp");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdDate = _super.createdDate;

    public final NumberPath<Long> id = createNumber("id", Long.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedDate = _super.updatedDate;

    public final com.hong.forapw.domain.user.entity.QUser user;

    public final StringPath userAgent = createString("userAgent");

    public QLoginAttempt(String variable) {
        this(LoginAttempt.class, forVariable(variable), INITS);
    }

    public QLoginAttempt(Path<? extends LoginAttempt> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QLoginAttempt(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QLoginAttempt(PathMetadata metadata, PathInits inits) {
        this(LoginAttempt.class, metadata, inits);
    }

    public QLoginAttempt(Class<? extends LoginAttempt> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.user = inits.isInitialized("user") ? new com.hong.forapw.domain.user.entity.QUser(forProperty("user"), inits.get("user")) : null;
    }

}

