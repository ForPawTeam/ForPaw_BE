package com.hong.forapw.domain.post.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QPost is a Querydsl query type for Post
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QPost extends EntityPathBase<Post> {

    private static final long serialVersionUID = 1648128025L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QPost post = new QPost("post");

    public final com.hong.forapw.common.entity.QBaseEntity _super = new com.hong.forapw.common.entity.QBaseEntity(this);

    public final NumberPath<Long> answerNum = createNumber("answerNum", Long.class);

    public final ListPath<Post, QPost> children = this.<Post, QPost>createList("children", Post.class, QPost.class, PathInits.DIRECT2);

    public final NumberPath<Long> commentNum = createNumber("commentNum", Long.class);

    public final StringPath content = createString("content");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdDate = _super.createdDate;

    public final com.hong.forapw.domain.group.entity.QGroup group;

    public final NumberPath<Double> hotPoint = createNumber("hotPoint", Double.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final BooleanPath isBlocked = createBoolean("isBlocked");

    public final QPost parent;

    public final ListPath<PostImage, QPostImage> postImages = this.<PostImage, QPostImage>createList("postImages", PostImage.class, QPostImage.class, PathInits.DIRECT2);

    public final EnumPath<com.hong.forapw.domain.post.constant.PostType> postType = createEnum("postType", com.hong.forapw.domain.post.constant.PostType.class);

    public final NumberPath<Long> readCnt = createNumber("readCnt", Long.class);

    public final DateTimePath<java.time.LocalDateTime> removedAt = createDateTime("removedAt", java.time.LocalDateTime.class);

    public final StringPath title = createString("title");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedDate = _super.updatedDate;

    public final com.hong.forapw.domain.user.entity.QUser user;

    public QPost(String variable) {
        this(Post.class, forVariable(variable), INITS);
    }

    public QPost(Path<? extends Post> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QPost(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QPost(PathMetadata metadata, PathInits inits) {
        this(Post.class, metadata, inits);
    }

    public QPost(Class<? extends Post> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.group = inits.isInitialized("group") ? new com.hong.forapw.domain.group.entity.QGroup(forProperty("group")) : null;
        this.parent = inits.isInitialized("parent") ? new QPost(forProperty("parent"), inits.get("parent")) : null;
        this.user = inits.isInitialized("user") ? new com.hong.forapw.domain.user.entity.QUser(forProperty("user"), inits.get("user")) : null;
    }

}

