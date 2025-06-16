package com.hong.forapw.domain.chat.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QChatUser is a Querydsl query type for ChatUser
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QChatUser extends EntityPathBase<ChatUser> {

    private static final long serialVersionUID = 58230004L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QChatUser chatUser = new QChatUser("chatUser");

    public final com.hong.forapw.common.entity.QBaseEntity _super = new com.hong.forapw.common.entity.QBaseEntity(this);

    public final QChatRoom chatRoom;

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdDate = _super.createdDate;

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final StringPath lastReadMessageId = createString("lastReadMessageId");

    public final NumberPath<Long> lastReadMessageIndex = createNumber("lastReadMessageIndex", Long.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedDate = _super.updatedDate;

    public final com.hong.forapw.domain.user.entity.QUser user;

    public QChatUser(String variable) {
        this(ChatUser.class, forVariable(variable), INITS);
    }

    public QChatUser(Path<? extends ChatUser> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QChatUser(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QChatUser(PathMetadata metadata, PathInits inits) {
        this(ChatUser.class, metadata, inits);
    }

    public QChatUser(Class<? extends ChatUser> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.chatRoom = inits.isInitialized("chatRoom") ? new QChatRoom(forProperty("chatRoom"), inits.get("chatRoom")) : null;
        this.user = inits.isInitialized("user") ? new com.hong.forapw.domain.user.entity.QUser(forProperty("user"), inits.get("user")) : null;
    }

}

