package com.hong.forapw.domain.post.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QPopularPost is a Querydsl query type for PopularPost
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QPopularPost extends EntityPathBase<PopularPost> {

    private static final long serialVersionUID = 856320384L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QPopularPost popularPost = new QPopularPost("popularPost");

    public final com.hong.forapw.common.entity.QBaseEntity _super = new com.hong.forapw.common.entity.QBaseEntity(this);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdDate = _super.createdDate;

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final QPost post;

    public final EnumPath<com.hong.forapw.domain.post.constant.PostType> postType = createEnum("postType", com.hong.forapw.domain.post.constant.PostType.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedDate = _super.updatedDate;

    public QPopularPost(String variable) {
        this(PopularPost.class, forVariable(variable), INITS);
    }

    public QPopularPost(Path<? extends PopularPost> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QPopularPost(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QPopularPost(PathMetadata metadata, PathInits inits) {
        this(PopularPost.class, metadata, inits);
    }

    public QPopularPost(Class<? extends PopularPost> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.post = inits.isInitialized("post") ? new QPost(forProperty("post"), inits.get("post")) : null;
    }

}

