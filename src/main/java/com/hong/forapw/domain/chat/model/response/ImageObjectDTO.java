package com.hong.forapw.domain.chat.model.response;

import com.hong.forapw.domain.chat.entity.Message;

import java.time.LocalDateTime;
import java.util.List;

public record ImageObjectDTO(
        String messageId,
        String nickName,
        String profileURL,
        List<ChatObjectDTO> objects,
        LocalDateTime date
) {

    public static ImageObjectDTO fromEntity(Message message) {
        List<ChatObjectDTO> chatObjectDTOs = message.getObjectURLs().stream()
                .map(ChatObjectDTO::new)
                .toList();

        return new ImageObjectDTO(
                message.getId(),
                message.getNickName(),
                message.getProfileURL(),
                chatObjectDTOs,
                message.getDate());
    }
}