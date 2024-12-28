package com.hong.forapw.domain.chat.model.response;

import com.hong.forapw.domain.chat.entity.LinkMetadata;
import com.hong.forapw.domain.chat.entity.Message;

import java.time.LocalDateTime;
import java.util.List;

public record FindLinkObjectsRes(
        List<LinkObjectDTO> links,
        boolean isLastPage
) {

    public record LinkObjectDTO(
            String messageId,
            String title,
            String description,
            String image,
            String ogUrl,
            LocalDateTime date
    ) {

        public static LinkObjectDTO fromEntity(Message message) {
            LinkMetadata metadata = message.getMetadata();
            String title = metadata != null ? metadata.getTitle() : null;
            String description = metadata != null ? metadata.getDescription() : null;
            String image = metadata != null ? metadata.getImage() : null;
            String ogUrl = metadata != null ? metadata.getOgUrl() : null;

            return new LinkObjectDTO(message.getId(), title, description, image, ogUrl, message.getDate());
        }
    }
}