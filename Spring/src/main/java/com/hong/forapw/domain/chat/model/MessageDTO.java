package com.hong.forapw.domain.chat.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.hong.forapw.domain.chat.constant.MessageType;
import com.hong.forapw.domain.chat.entity.LinkMetadata;
import com.hong.forapw.domain.chat.entity.Message;
import com.hong.forapw.domain.chat.model.request.ChatObjectDTO;
import com.hong.forapw.domain.chat.model.request.SendMessageReq;

import java.time.LocalDateTime;
import java.util.List;

public record MessageDTO(
        String messageId,
        String nickName,
        String profileURL,
        String content,
        MessageType messageType,
        List<ChatObjectDTO> objects,
        @JsonSerialize(using = LocalDateTimeSerializer.class)
        @JsonDeserialize(using = LocalDateTimeDeserializer.class)
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss.SSS")
        LocalDateTime date,
        Long chatRoomId,
        Long senderId,
        LinkMetadata linkMetadata
) {

    public MessageDTO(SendMessageReq request, String senderNickName, String messageId, LinkMetadata metadata, String profileURL, Long senderId) {
        this(
                messageId,
                senderNickName,
                profileURL,
                request.content(),
                (metadata != null) ? MessageType.LINK : request.messageType(),
                request.objects(),
                LocalDateTime.now(),
                request.chatRoomId(),
                senderId,
                metadata);
    }

    public Message toEntity(List<String> objectURLs) {
        return Message.builder()
                .id(messageId)
                .nickName(nickName)
                .profileURL(profileURL)
                .content(content)
                .messageType(messageType)
                .objectURLs(objectURLs)
                .date(date)
                .chatRoomId(chatRoomId)
                .senderId(senderId)
                .metadata(linkMetadata)
                .build();
    }
}
