package com.hong.forapw.domain.post.entity;

import com.hong.forapw.common.entity.BaseEntity;
import com.hong.forapw.common.exceptions.CustomException;
import com.hong.forapw.common.exceptions.ExceptionCode;
import com.hong.forapw.domain.region.constant.Province;
import com.hong.forapw.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.SQLDelete;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "comment_tb")
@SQLDelete(sql = "UPDATE comment_tb SET removed_at = NOW() WHERE id=?")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class Comment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id")
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id") // 부모 댓글을 가리키는 외래키
    private Comment parent;

    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = true)
    @BatchSize(size = 100)
    private List<Comment> children = new ArrayList<>();

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(name = "removed_at")
    private LocalDateTime removedAt;

    @Builder
    public Comment(User user, Post post, String content) {
        this.user = user;
        this.post = post;
        this.content = content;
    }

    // 연관 관계 메서드
    public void addChildComment(Comment child) {
        this.children.add(child);
        child.updateParent(this);
    }

    public void updateParent(Comment parent) {
        this.parent = parent;
    }

    public void updateContent(String content) {
        this.content = content;
    }

    public String getWriterNickname() {
        return user.getNickname();
    }

    public String getWriterProfileURL() {
        return user.getProfileURL();
    }

    public Province getWriterProvince() {
        return user.getProvince();
    }

    public String getParentWriterNickname() {
        return parent.getUser().getNickname();
    }

    public Long getParentId() {
        return parent.getId();
    }

    public Long getPostId() {
        return post.getId();
    }

    public String getPostTitle() {
        return post.getTitle();
    }

    public Long getPostCommentNumber() {
        return post.getCommentNum();
    }

    public String getPostTypeValue(){
        return post.getPostType().getValue();
    }

    public String getPostTypeName(){
        return post.getPostType().name();
    }

    public Long getWriterId() {
        return user.getId();
    }

    public long getReplyCount(){
        return children.size();
    }

    public boolean isPostBlocked(){
        return post.isBlocked();
    }

    public boolean isParent() {
        return parent == null;
    }

    public boolean isReply() {
        return parent != null;
    }

    public boolean isNotBelongToPost(Long postId) {
        return !this.post.getId().equals(postId);
    }

    public void validateParentCommentState(Long postId) {
        if (this.isReply()) {
            throw new CustomException(ExceptionCode.CANT_REPLY_TO_REPLY);
        }

        if (this.isNotBelongToPost(postId)) {
            throw new CustomException(ExceptionCode.NOT_POSTS_COMMENT);
        }
    }

    public void validateCommentBelongsToPost(Long postId) {
        if (this.isNotBelongToPost(postId)) {
            throw new CustomException(ExceptionCode.NOT_POSTS_COMMENT);
        }
    }
}
