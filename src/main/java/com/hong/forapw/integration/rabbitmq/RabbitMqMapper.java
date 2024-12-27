package com.hong.forapw.integration.rabbitmq;

import com.hong.forapw.domain.chat.model.ChatRequest;
import com.hong.forapw.domain.chat.entity.Message;

import java.util.List;

public class RabbitMqMapper {

    private RabbitMqMapper() {
    }

    public static Message buildMessage(ChatRequest.MessageDTO messageDTO, List<String> objectURLs) {
        return Message.builder()
                .id(messageDTO.messageId())
                .nickName(messageDTO.nickName())
                .profileURL(messageDTO.profileURL())
                .content(messageDTO.content())
                .messageType(messageDTO.messageType())
                .objectURLs(objectURLs)
                .date(messageDTO.date())
                .chatRoomId(messageDTO.chatRoomId())
                .senderId(messageDTO.senderId())
                .metadata(messageDTO.linkMetadata())
                .build();
    }
}
