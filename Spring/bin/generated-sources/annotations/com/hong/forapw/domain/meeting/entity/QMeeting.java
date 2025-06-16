package com.hong.forapw.domain.meeting.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QMeeting is a Querydsl query type for Meeting
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QMeeting extends EntityPathBase<Meeting> {

    private static final long serialVersionUID = 669671071L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QMeeting meeting = new QMeeting("meeting");

    public final com.hong.forapw.common.entity.QBaseEntity _super = new com.hong.forapw.common.entity.QBaseEntity(this);

    public final NumberPath<Long> cost = createNumber("cost", Long.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdDate = _super.createdDate;

    public final com.hong.forapw.domain.user.entity.QUser creator;

    public final StringPath description = createString("description");

    public final com.hong.forapw.domain.group.entity.QGroup group;

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final StringPath location = createString("location");

    public final NumberPath<Integer> maxNum = createNumber("maxNum", Integer.class);

    public final DateTimePath<java.time.LocalDateTime> meetDate = createDateTime("meetDate", java.time.LocalDateTime.class);

    public final ListPath<MeetingUser, QMeetingUser> meetingUsers = this.<MeetingUser, QMeetingUser>createList("meetingUsers", MeetingUser.class, QMeetingUser.class, PathInits.DIRECT2);

    public final StringPath name = createString("name");

    public final NumberPath<Long> participantNum = createNumber("participantNum", Long.class);

    public final StringPath profileURL = createString("profileURL");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedDate = _super.updatedDate;

    public QMeeting(String variable) {
        this(Meeting.class, forVariable(variable), INITS);
    }

    public QMeeting(Path<? extends Meeting> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QMeeting(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QMeeting(PathMetadata metadata, PathInits inits) {
        this(Meeting.class, metadata, inits);
    }

    public QMeeting(Class<? extends Meeting> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.creator = inits.isInitialized("creator") ? new com.hong.forapw.domain.user.entity.QUser(forProperty("creator"), inits.get("creator")) : null;
        this.group = inits.isInitialized("group") ? new com.hong.forapw.domain.group.entity.QGroup(forProperty("group")) : null;
    }

}

