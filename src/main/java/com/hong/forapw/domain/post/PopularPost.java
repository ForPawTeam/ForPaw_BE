package com.hong.forapw.domain.post;

import com.hong.forapw.domain.TimeStamp;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;

@Entity
@Table(name = "popular_post_tb")
@SQLDelete(sql = "UPDATE popular_post_tb SET removed_at = NOW() WHERE id=?")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class PopularPost extends TimeStamp {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "post_id")
    private Post post;

    @Column
    private PostType postType;

    @Builder
    public PopularPost(Post post, PostType postType) {
        this.post = post;
        this.postType = postType;
    }
}
