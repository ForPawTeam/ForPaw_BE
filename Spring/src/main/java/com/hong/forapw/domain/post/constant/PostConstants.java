package com.hong.forapw.domain.post.constant;

import java.util.List;

public class PostConstants {

    private PostConstants() {}

    public static final String COMMENT_DELETED = "삭제된 댓글 입니다.";

    public static final int SHORT_TERM_DAYS = 3;  // 최근 3일
    public static final int MEDIUM_TERM_DAYS = 7; // 최근 7일
    public static final int LONG_TERM_DAYS = 30;  // 최근 30일
    public static final int POPULAR_POSTS_PER_TYPE = 5;  // 각 포스트 타입별 인기글 개수

    public static final List<PostType> POPULAR_POST_TYPES = List.of(
            PostType.ADOPTION,
            PostType.FOSTERING,
            PostType.QUESTION
    );
}
