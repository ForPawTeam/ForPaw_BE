package com.hong.ForPaw.controller.DTO;

import java.time.LocalDateTime;
import java.util.List;

public class ChatResponse {

    public record FindMessageListInRoomDTO(List<ChatResponse.MessageDTD> messages) {}

    public record MessageDTD(Long messageId,
                             String senderName,
                             String content,
                             LocalDateTime date,
                             boolean isMine) {}
}
