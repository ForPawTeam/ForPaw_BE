package com.hong.forapw.common.constants;

import com.hong.forapw.domain.group.constant.GroupRole;
import com.hong.forapw.domain.post.constant.PostType;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

public class GlobalConstants {

    private GlobalConstants() {
    }

    // JWT
    public static final String REFRESH_TOKEN_KEY = "refreshToken";
    public static final String ACCESS_TOKEN_KEY = "accessToken";

    // Redis Key
    public static final String ANIMAL_SEARCH_KEY = "animal:search";
    public static final String POST_READ_KEY = "user:readPosts";
    public static final String POST_LIKE_COUNT_KEY = "post:like:count";
    public static final String COMMENT_LIKE_COUNT_KEY = "comment:like:count";
    public static final String POST_VIEW_COUNT_KEY = "post:view:count";
    public static final String POST_LIKE_NUM_KEY = "post:like:count";
    public static final String POST_LIKED_SET_KEY = "user:%s:liked_posts";
    public static final String ANIMAL_LIKE_NUM_KEY = "animal:like:count";
    public static final String ANIMAL_LIKED_SET_KEY = "user:%s:liked_animals";
    public static final String COMMENT_LIKE_NUM_KEY = "comment:like:count";
    public static final String COMMENT_LIKED_SET_KEY = "user:%s:liked_comments";
    public static final String GROUP_LIKE_NUM_KEY = "group:like:count";
    public static final String GROUP_LIKED_SET_KEY = "user:%s:liked_groups";

    // Http
    public static final String AUTH_CODE_GRANT_TYPE = "authorization_code";
    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String BEARER_PREFIX = "Bearer ";
    public static final String USER_AGENT_HEADER = "User-Agent";
    public static final String[] IP_HEADER_CANDIDATES = {"X-Forwarded-For", "Proxy-Client-IP", "WL-Proxy-Client-IP", "HTTP_CLIENT_IP", "HTTP_X_FORWARDED_FOR"};

    // RabbitMQ
    public static final String ROOM_QUEUE_PREFIX = "room.";
    public static final String CHAT_EXCHANGE = "chat.exchange";

    // Sort
    public static final String SORT_BY_ID = "id";
    public static final String SORT_BY_PARTICIPANT_NUM = "participantNum";
    public static final String SORT_BY_MESSAGE_DATE = "date";
    public static final String SORT_BY_DATE = "createdDate";

    // Url Pattern
    public static final String URL_REGEX = "(https?://[\\w\\-\\._~:/?#\\[\\]@!$&'()*+,;=%]+)";
    public static final Pattern URL_PATTERN = Pattern.compile(URL_REGEX);

    // Mail Template
    public static final String MAIL_TEMPLATE_FOR_CODE = "verification_code_email.html";
    public static final String MAIL_TEMPLATE_FOR_LOCK_ACCOUNT = "lock_account.html";

    // Login Failure Limit
    public static final long CURRENT_FAILURE_LIMIT = 3L;
    public static final long DAILY_FAILURE_LIMIT = 3L;

    // else
    public static final List<PostType> QUESTION_TYPES = List.of(PostType.QUESTION);
    public static final List<PostType> MY_POST_TYPES = List.of(PostType.ADOPTION, PostType.FOSTERING);
    public static final List<PostType> ALL_POST_TYPES = List.of(PostType.ADOPTION, PostType.FOSTERING, PostType.QUESTION, PostType.ANSWER);
    public static final Set<GroupRole> GROUP_USER_TYPES = EnumSet.of(GroupRole.USER, GroupRole.ADMIN, GroupRole.CREATOR);
    public static final Set<GroupRole> GROUP_ADMIN_TYPES = EnumSet.of(GroupRole.ADMIN, GroupRole.CREATOR);

    public static final String ALL_CHARS = "!@#$%^&*0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
}