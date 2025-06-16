package com.hong.forapw.domain.inquiry.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QInquiry is a Querydsl query type for Inquiry
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QInquiry extends EntityPathBase<Inquiry> {

    private static final long serialVersionUID = -2083924705L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QInquiry inquiry = new QInquiry("inquiry");

    public final com.hong.forapw.common.entity.QBaseEntity _super = new com.hong.forapw.common.entity.QBaseEntity(this);

    public final StringPath answer = createString("answer");

    public final com.hong.forapw.domain.user.entity.QUser answerer;

    public final StringPath contactMail = createString("contactMail");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdDate = _super.createdDate;

    public final StringPath description = createString("description");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final StringPath imageURL = createString("imageURL");

    public final com.hong.forapw.domain.user.entity.QUser questioner;

    public final EnumPath<com.hong.forapw.domain.inquiry.constant.InquiryStatus> status = createEnum("status", com.hong.forapw.domain.inquiry.constant.InquiryStatus.class);

    public final StringPath title = createString("title");

    public final EnumPath<com.hong.forapw.domain.inquiry.constant.InquiryType> type = createEnum("type", com.hong.forapw.domain.inquiry.constant.InquiryType.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedDate = _super.updatedDate;

    public QInquiry(String variable) {
        this(Inquiry.class, forVariable(variable), INITS);
    }

    public QInquiry(Path<? extends Inquiry> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QInquiry(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QInquiry(PathMetadata metadata, PathInits inits) {
        this(Inquiry.class, metadata, inits);
    }

    public QInquiry(Class<? extends Inquiry> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.answerer = inits.isInitialized("answerer") ? new com.hong.forapw.domain.user.entity.QUser(forProperty("answerer"), inits.get("answerer")) : null;
        this.questioner = inits.isInitialized("questioner") ? new com.hong.forapw.domain.user.entity.QUser(forProperty("questioner"), inits.get("questioner")) : null;
    }

}

