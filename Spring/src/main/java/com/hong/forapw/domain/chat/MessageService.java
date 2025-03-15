package com.hong.forapw.domain.chat;

import com.hong.forapw.domain.chat.entity.Message;
import com.hong.forapw.domain.chat.model.MessageDTO;
import com.hong.forapw.domain.chat.model.request.ChatObjectDTO;
import com.hong.forapw.domain.chat.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageRepository messageRepository;

    public void saveMessage(MessageDTO messageDTO) {
        List<String> objectURLs = Optional.ofNullable(messageDTO.objects())
                .orElse(Collections.emptyList())
                .stream()
                .map(ChatObjectDTO::objectURL)
                .toList();

        Message message = messageDTO.toEntity(objectURLs);
        messageRepository.save(message);
    }
}
