package com.hong.forapw.domain.chat.model.response;

import com.hong.forapw.domain.chat.entity.ChatUser;
import com.hong.forapw.domain.chat.model.MessageDetailDTO;

import java.time.LocalDateTime;
import java.util.List;

public record FindChatRoomsRes(List<RoomDTO> rooms) {

    public record RoomDTO(
            Long chatRoomId,
            String name,
            String lastMessageContent,
            LocalDateTime lastMessageTime,
            Long offset,
            String profileURL
    ) {

        public RoomDTO(ChatUser chatUser, MessageDetailDTO lastMessageDetails, long offset) {
            this(
                    chatUser.getChatRoomId(),
                    chatUser.getRoomName(),
                    lastMessageDetails.content(),
                    lastMessageDetails.date(),
                    offset,
                    chatUser.getGroupProfileURL());
        }
    }
}