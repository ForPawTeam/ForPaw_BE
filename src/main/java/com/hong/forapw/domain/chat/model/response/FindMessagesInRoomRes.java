package com.hong.forapw.domain.chat.model.response;

import com.hong.forapw.domain.chat.constant.MessageType;
import com.hong.forapw.domain.chat.entity.LinkMetadata;
import com.hong.forapw.domain.chat.entity.Message;

import java.time.LocalDateTime;
import java.util.List;

import static com.hong.forapw.domain.chat.ChatMapper.toMessageDTO;

public record FindMessagesInRoomRes(
        String chatRoomName,
        String lastMessageId,
        String myNickName,
        List<MessageDTO> messages
) {
    public record MessageDTO(
            String messageId,
            String nickName,
            String profileURL,
            String content,
            MessageType messageType,
            List<ChatObjectDTO> objects,
            LinkMetadata linkMetadata,
            LocalDateTime date,
            boolean isMine
    ) {

        public static MessageDTO fromEntity(Message message, Long userId) {
            List<ChatObjectDTO> imageDTOs = message.getObjectURLs().stream()
                    .map(ChatObjectDTO::new)
                    .toList();

            return new MessageDTO(
                    message.getId(),
                    message.getNickName(),
                    message.getProfileURL(),
                    message.getContent(),
                    message.getMessageType(),
                    imageDTOs,
                    message.getMetadata(),
                    message.getDate(),
                    message.getSenderId().equals(userId));
        }

        public static List<MessageDTO> fromEntities(List<Message> messages, Long userId) {
            return messages.stream()
                    .map(message -> MessageDTO.fromEntity(message, userId))
                    .toList();
        }
    }
}
