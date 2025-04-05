package com.hong.forapw.integration.rabbitmq;

public class RabbitMqConstants {

    private RabbitMqConstants() {}

    public static final String ROOM_QUEUE_PREFIX = "room.";
    public static final String CHAT_EXCHANGE = "chat.exchange";

    public static final String X_DEATH_HEADER = "x-death";
    public static final String COUNT_PROPERTY = "count";
    public static final int MAX_RETRY_COUNT = 10;
}