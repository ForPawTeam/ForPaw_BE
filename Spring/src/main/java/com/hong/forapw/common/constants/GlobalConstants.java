package com.hong.forapw.common.constants;

import com.hong.forapw.domain.post.constant.PostType;
import com.hong.forapw.domain.region.constant.Province;

import java.util.List;
import java.util.regex.Pattern;

public class GlobalConstants {

    private GlobalConstants() {
    }

    // JWT
    public static final String REFRESH_TOKEN_KEY = "refreshToken";
    public static final String ACCESS_TOKEN_KEY = "accessToken";

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

    // Types
    public static final List<PostType> QUESTION_TYPES = List.of(PostType.QUESTION);
    public static final List<PostType> MY_POST_TYPES = List.of(PostType.ADOPTION, PostType.FOSTERING);
    public static final List<PostType> ALL_POST_TYPES = List.of(PostType.ADOPTION, PostType.FOSTERING, PostType.QUESTION, PostType.ANSWER);

    // Default
    public static final Long DEFAULT_VALUE = 0L;
    public static final Province DEFAULT_PROVINCE = Province.DAEGU;
}