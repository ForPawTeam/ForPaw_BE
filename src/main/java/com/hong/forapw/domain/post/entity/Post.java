package com.hong.forapw.domain.post.entity;

import com.hong.forapw.common.entity.BaseEntity;
import com.hong.forapw.common.exceptions.CustomException;
import com.hong.forapw.common.exceptions.ExceptionCode;
import com.hong.forapw.domain.group.entity.Group;
import com.hong.forapw.domain.group.model.request.CreateNoticeReq;
import com.hong.forapw.domain.post.constant.PostType;
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
@Table(name = "post_tb")
@SQLDelete(sql = "UPDATE post_tb SET removed_at = NOW() WHERE id=?")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class Post extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id")
    private Group group;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id")
    private Post parent;

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    @BatchSize(size = 10)
    private List<PostImage> postImages = new ArrayList<>();

    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = true)
    @BatchSize(size = 10)
    private List<Post> children = new ArrayList<>();

    @Column
    @Enumerated(EnumType.STRING)
    private PostType postType;

    @Column
    private String title;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column
    private Long answerNum = 0L;

    @Column
    private Long commentNum = 0L;

    @Column
    private Long readCnt = 0L;

    @Column
    private Double hotPoint = 0.0;

    @Column
    private boolean isBlocked;

    @Column(name = "removed_at")
    private LocalDateTime removedAt;

    @Builder
    public Post(User user, Group group, PostType postType, String title, String content) {
        this.user = user;
        this.group = group;
        this.postType = postType;
        this.title = title;
        this.content = content;
        this.isBlocked = false;
    }

    public void addImage(PostImage postImage) {
        this.postImages.add(postImage);
        postImage.updatePost(this);
    }

    public void addChildPost(Post child) {
        this.children.add(child);
        child.updateParent(this);
    }

    public void addImages(List<CreateNoticeReq.PostImageDTO> images) {
        images.stream()
                .map(imageDTO -> PostImage.builder().imageURL(imageDTO.imageURL()).build())
                .forEach(this::addImage);
    }

    public void setAnswerRelationships(List<PostImage> answerImages, Post questionPost) {
        answerImages.forEach(this::addImage); // 이미지와 답변 게시물의 연관 설정
        questionPost.addChildPost(this); // 질문 게시물과 답변 게시물의 부모-자식 관계 설정
    }

    public void setPostRelationships(List<PostImage> postImages) {
        postImages.forEach(this::addImage);
    }

    public void updateContent(String title, String content) {
        this.title = title;
        this.content = content;
    }

    public void updateTitle(String title) {
        this.title = title;
    }

    public void updateParent(Post parent) {
        this.parent = parent;
    }

    public void updateReadCnt(Long readCnt) {
        this.readCnt = readCnt;
    }

    public void updateHotPoint(Double hotPoint) {
        this.hotPoint = hotPoint;
    }

    public void processBlock() {
        this.isBlocked = true;
    }

    public void incrementAnswerNum() {
        this.answerNum++;
    }

    public String getFirstImageURL() {
        return postImages.isEmpty() ? null : postImages.get(0).getImageURL();
    }

    public Long getWriterId() {
        return user.getId();
    }

    public String getWriterNickName() {
        return user.getNickname();
    }

    public String getWriterProfileURL() {
        return user.getProfileURL();
    }

    public String getPostTypeString() {
        return postType.toString().toLowerCase();
    }

    public boolean isOwner(Long userId) {
        return user.getId().equals(userId);
    }

    public boolean isQuestionType() {
        return postType == PostType.QUESTION;
    }

    public boolean isNotQuestionType() {
        return postType != PostType.QUESTION;
    }

    public boolean isNoticeType() {
        return postType == PostType.NOTICE;
    }

    public boolean isNotAnswerType() {
        return postType != PostType.ANSWER;
    }

    public boolean isScreened() {
        return title.equals("이 게시글은 커뮤니티 규정을 위반하여 숨겨졌습니다.");
    }

    public void validateQuestionType() {
        if (this.isNotQuestionType()) {
            throw new CustomException(ExceptionCode.NOT_QUESTION_TYPE);
        }
    }

    public void validatePostState() {
        if (this.isQuestionType()) {
            throw new CustomException(ExceptionCode.NOT_QUESTION_TYPE);
        }

        if (this.isScreened()) {
            throw new CustomException(ExceptionCode.SCREENED_POST);
        }
    }

    public void validateQnaState() {
        if (this.isNotQuestionType()) {
            throw new CustomException(ExceptionCode.NOT_QUESTION_TYPE);
        }

        if (this.isScreened()) {
            throw new CustomException(ExceptionCode.SCREENED_POST);
        }
    }

    public void validateAnswerType() {
        if (this.isNotAnswerType()) {
            throw new CustomException(ExceptionCode.NOT_ANSWER_TYPE);
        }
    }
}