package com.hong.forapw.common.constants;

public class GlobalConstants {

    private GlobalConstants() {
    }

    // Jwt Token
    public static final String REFRESH_TOKEN_KEY = "refreshToken";
    public static final String ACCESS_TOKEN_KEY = "accessToken";

    public static final String AUTH_CODE_GRANT_TYPE = "authorization_code";
    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String BEARER_PREFIX = "Bearer ";

    // RabbitMQ
    public static final String ROOM_QUEUE_PREFIX = "room.";
    public static final String CHAT_EXCHANGE = "chat.exchange";

    // Sort
    public static final String SORT_BY_ID = "id";
    public static final String SORT_BY_DATE = "date";
    public static final String SORT_BY_PARTICIPANT_NUM = "participantNum";
}