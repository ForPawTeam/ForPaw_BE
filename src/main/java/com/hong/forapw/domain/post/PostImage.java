package com.hong.forapw.domain.post;

import com.hong.forapw.domain.TimeStamp;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;


@Entity
@Table(name = "postImage_tb")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class PostImage extends TimeStamp {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id")
    private Post post;

    @Column
    private String imageURL;

    @Builder
    public PostImage(Post post, String imageURL) {
        this.post = post;
        this.imageURL = imageURL;
    }

    public void updatePost(Post post) {
        this.post = post;
    }
}