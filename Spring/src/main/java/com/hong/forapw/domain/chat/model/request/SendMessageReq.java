package com.hong.forapw.domain.chat.model.request;

import com.hong.forapw.domain.chat.constant.MessageType;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record SendMessageReq(
        @NotNull(message = "채팅방 ID를 입력해주세요.")
        Long chatRoomId,
        @NotNull(message = "내용을 입력해주세요.")
        String content,
        MessageType messageType,
        List<ChatObjectDTO> objects) {
}
