package com.hong.forapw.domain.post.entity;

import com.hong.forapw.common.entity.BaseEntity;
import com.hong.forapw.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;


@Entity
@Table(name = "postLike_tb", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "post_id"})
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class PostLike extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id")
    private Post post;

    @Builder
    public PostLike(User user, Post post) {
        this.user = user;
        this.post = post;
    }
}