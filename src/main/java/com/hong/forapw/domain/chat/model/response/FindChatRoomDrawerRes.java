package com.hong.forapw.domain.chat.model.response;

import com.hong.forapw.domain.user.entity.User;

import java.util.List;

public record FindChatRoomDrawerRes(
        List<ImageObjectDTO> images,
        List<ChatUserDTO> users
) {

    public record ChatUserDTO(
            Long userId,
            String nickName,
            String profileURL
    ) {

        public ChatUserDTO(User user) {
            this(
                    user.getId(),
                    user.getNickname(),
                    user.getProfileURL()
            );
        }
    }
}