package com.hong.forapw.admin.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QReport is a Querydsl query type for Report
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QReport extends EntityPathBase<Report> {

    private static final long serialVersionUID = -111192334L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QReport report = new QReport("report");

    public final com.hong.forapw.common.entity.QBaseEntity _super = new com.hong.forapw.common.entity.QBaseEntity(this);

    public final NumberPath<Long> contentId = createNumber("contentId", Long.class);

    public final EnumPath<com.hong.forapw.admin.constant.ContentType> contentType = createEnum("contentType", com.hong.forapw.admin.constant.ContentType.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdDate = _super.createdDate;

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final com.hong.forapw.domain.user.entity.QUser offender;

    public final StringPath reason = createString("reason");

    public final com.hong.forapw.domain.user.entity.QUser reporter;

    public final EnumPath<com.hong.forapw.admin.constant.ReportStatus> status = createEnum("status", com.hong.forapw.admin.constant.ReportStatus.class);

    public final EnumPath<com.hong.forapw.admin.constant.ReportType> type = createEnum("type", com.hong.forapw.admin.constant.ReportType.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedDate = _super.updatedDate;

    public QReport(String variable) {
        this(Report.class, forVariable(variable), INITS);
    }

    public QReport(Path<? extends Report> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QReport(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QReport(PathMetadata metadata, PathInits inits) {
        this(Report.class, metadata, inits);
    }

    public QReport(Class<? extends Report> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.offender = inits.isInitialized("offender") ? new com.hong.forapw.domain.user.entity.QUser(forProperty("offender"), inits.get("offender")) : null;
        this.reporter = inits.isInitialized("reporter") ? new com.hong.forapw.domain.user.entity.QUser(forProperty("reporter"), inits.get("reporter")) : null;
    }

}

