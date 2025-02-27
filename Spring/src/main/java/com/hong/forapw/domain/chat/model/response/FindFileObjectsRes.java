package com.hong.forapw.domain.chat.model.response;

import com.hong.forapw.domain.chat.entity.Message;

import java.time.LocalDateTime;
import java.util.List;

public record FindFileObjectsRes(
        List<FileObjectDTO> files,
        boolean isLastPage
) {

    public record FileObjectDTO(
            String messageId,
            String fileName,
            List<ChatObjectDTO> objects,
            LocalDateTime date
    ) {

        public static FileObjectDTO fromEntity(Message message) {
            List<ChatObjectDTO> chatObjectDTOS = message.getObjectURLs().stream()
                    .map(ChatObjectDTO::new)
                    .toList();

            return new FileObjectDTO(
                    message.getId(),
                    message.getContent(),
                    chatObjectDTOS,
                    message.getDate());
        }
    }
}