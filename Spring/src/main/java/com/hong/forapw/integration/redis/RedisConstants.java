package com.hong.forapw.integration.redis;

public class RedisConstants {

    private RedisConstants() {}

    public static final Long POST_CACHE_EXPIRATION = 1000L * 60 * 60 * 24 * 90; // 게시글 좋아요/뷰 카운트를 캐싱하는 기간 (3개월)
    public static final Long NOTICE_READ_EXPIRATION = 60L * 60 * 24 * 360; // 공지사항 읽음 상태 캐싱 기간 (1년)

    public static final long VERIFICATION_CODE_EXPIRATION_MS = 175 * 1000L;
    public static final long LOGIN_FAIL_CURRENT_EXPIRATION_MS = 300_000L; // 5분
    public static final long LOGIN_FAIL_DAILY_EXPIRATION_MS = 86400000L; // 24시간

    // Redis Key
    public static final String ANIMAL_SEARCH_KEY = "animal:search";
    public static final String POST_READ_KEY = "user:readPosts";
    public static final String POST_VIEW_COUNT_KEY = "post:view:count";
    public static final String POST_LIKE_NUM_KEY = "post:like:count";
    public static final String POST_LIKED_SET_KEY = "user:%s:liked_posts";
    public static final String ANIMAL_LIKE_NUM_KEY = "animal:like:count";
    public static final String ANIMAL_LIKED_SET_KEY = "user:%s:liked_animals";
    public static final String COMMENT_LIKE_NUM_KEY = "comment:like:count";
    public static final String COMMENT_LIKED_SET_KEY = "user:%s:liked_comments";
    public static final String GROUP_LIKE_NUM_KEY = "group:like:count";
    public static final String GROUP_LIKED_SET_KEY = "user:%s:liked_groups";
    public static final String USER_ANIMAL_INTERACTION_KEY = "userAnimalInteraction";

    public static final String EMAIL_CODE_KEY_PREFIX = "code:";
    public static final String CODE_TO_EMAIL_KEY_PREFIX = "codeToEmail";
    
    public static final String MAX_LOGIN_ATTEMPTS_BEFORE_LOCK_KEY = "loginFail";
    public static final String MAX_DAILY_LOGIN_FAILURES_KEY = "loginFailDaily";
}
