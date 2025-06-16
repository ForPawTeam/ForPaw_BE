package com.hong.forapw.domain.meeting.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QMeetingUser is a Querydsl query type for MeetingUser
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QMeetingUser extends EntityPathBase<MeetingUser> {

    private static final long serialVersionUID = 1484019466L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QMeetingUser meetingUser = new QMeetingUser("meetingUser");

    public final com.hong.forapw.common.entity.QBaseEntity _super = new com.hong.forapw.common.entity.QBaseEntity(this);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdDate = _super.createdDate;

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final QMeeting meeting;

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedDate = _super.updatedDate;

    public final com.hong.forapw.domain.user.entity.QUser user;

    public QMeetingUser(String variable) {
        this(MeetingUser.class, forVariable(variable), INITS);
    }

    public QMeetingUser(Path<? extends MeetingUser> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QMeetingUser(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QMeetingUser(PathMetadata metadata, PathInits inits) {
        this(MeetingUser.class, metadata, inits);
    }

    public QMeetingUser(Class<? extends MeetingUser> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.meeting = inits.isInitialized("meeting") ? new QMeeting(forProperty("meeting"), inits.get("meeting")) : null;
        this.user = inits.isInitialized("user") ? new com.hong.forapw.domain.user.entity.QUser(forProperty("user"), inits.get("user")) : null;
    }

}

