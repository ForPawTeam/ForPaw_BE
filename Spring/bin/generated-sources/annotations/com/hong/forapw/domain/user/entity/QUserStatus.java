package com.hong.forapw.domain.user.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QUserStatus is a Querydsl query type for UserStatus
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QUserStatus extends EntityPathBase<UserStatus> {

    private static final long serialVersionUID = -921896255L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QUserStatus userStatus = new QUserStatus("userStatus");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final BooleanPath isActive = createBoolean("isActive");

    public final NumberPath<Long> suspensionDays = createNumber("suspensionDays", Long.class);

    public final StringPath suspensionReason = createString("suspensionReason");

    public final DateTimePath<java.time.LocalDateTime> suspensionStart = createDateTime("suspensionStart", java.time.LocalDateTime.class);

    public final QUser user;

    public QUserStatus(String variable) {
        this(UserStatus.class, forVariable(variable), INITS);
    }

    public QUserStatus(Path<? extends UserStatus> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QUserStatus(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QUserStatus(PathMetadata metadata, PathInits inits) {
        this(UserStatus.class, metadata, inits);
    }

    public QUserStatus(Class<? extends UserStatus> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.user = inits.isInitialized("user") ? new QUser(forProperty("user"), inits.get("user")) : null;
    }

}

