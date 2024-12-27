package com.hong.forapw.integration.rabbitmq;

import com.hong.forapw.domain.alarm.AlarmService;
import com.hong.forapw.domain.alarm.constant.AlarmType;
import com.hong.forapw.domain.chat.model.ChatRequest;
import com.hong.forapw.domain.chat.entity.Message;
import com.hong.forapw.domain.group.constant.GroupRole;
import com.hong.forapw.domain.user.entity.User;
import com.hong.forapw.domain.chat.repository.ChatRoomRepository;
import com.hong.forapw.domain.chat.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerEndpoint;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.RabbitListenerEndpointRegistry;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static com.hong.forapw.integration.rabbitmq.RabbitMqMapper.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class RabbitMqService {

    private final ChatRoomRepository chatRoomRepository;
    private final MessageRepository messageRepository;
    private final RabbitListenerEndpointRegistry rabbitListenerEndpointRegistry;
    private final SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory;
    private final RabbitTemplate rabbitTemplate;
    private final AmqpAdmin amqpAdmin;
    private final MessageConverter converter;
    private final AlarmService alarmService;

    private static final String CHAT_EXCHANGE = "chat.exchange";
    private static final String ROOM_QUEUE_PREFIX = "room.";

    @Transactional
    public void initChatListener() {
        chatRoomRepository.findAll()
                .forEach(chatRoom -> {
                    String queueName = ROOM_QUEUE_PREFIX + chatRoom.getId();
                    String listenerId = ROOM_QUEUE_PREFIX + chatRoom.getId();

                    bindDirectExchangeToQueue(CHAT_EXCHANGE, queueName);
                    registerChatListener(listenerId, queueName);
                });
    }

    public void registerDirectExchange(String exchangeName) {
        DirectExchange fanoutExchange = new DirectExchange(exchangeName);
        amqpAdmin.declareExchange(fanoutExchange);
    }

    public void bindDirectExchangeToQueue(String exchangeName, String queueName) {
        DirectExchange directExchange = new DirectExchange(exchangeName);

        Queue queue = new Queue(queueName, true);
        amqpAdmin.declareQueue(queue);

        Binding binding = BindingBuilder.bind(queue).to(directExchange).with(queueName); // routingKey는 큐 이름과 동일하게 사용
        amqpAdmin.declareBinding(binding);
    }

    public void registerChatListener(String listenerId, String queueName) {
        SimpleRabbitListenerEndpoint endpoint = createRabbitListenerEndpoint(listenerId, queueName);

        endpoint.setMessageListener(m -> {
            ChatRequest.MessageDTO messageDTO = convertToMessageDTO(m);
            saveMessage(messageDTO);
            notifyChatRoomUsers(messageDTO);
        });

        rabbitListenerEndpointRegistry.registerListenerContainer(endpoint, rabbitListenerContainerFactory, true);
    }

    public void sendChatMessageToRoom(Long chatRoomId, ChatRequest.MessageDTO message) {
        String routingKey = ROOM_QUEUE_PREFIX + chatRoomId;
        rabbitTemplate.convertAndSend(CHAT_EXCHANGE, routingKey, message);
    }

    public void deleteQueue(String queueName) {
        amqpAdmin.deleteQueue(queueName);
    }

    private ChatRequest.MessageDTO convertToMessageDTO(org.springframework.amqp.core.Message m) {
        return (ChatRequest.MessageDTO) converter.fromMessage(m);
    }

    private void saveMessage(ChatRequest.MessageDTO messageDTO) {
        List<String> objectURLs = Optional.ofNullable(messageDTO.objects())
                .orElse(Collections.emptyList())
                .stream()
                .map(ChatRequest.ChatObjectDTO::objectURL)
                .toList();

        Message message = buildMessage(messageDTO, objectURLs);
        messageRepository.save(message);
    }

    private void notifyChatRoomUsers(ChatRequest.MessageDTO messageDTO) {
        List<User> users = chatRoomRepository.findUsersByChatRoomIdExcludingRole(messageDTO.chatRoomId(), GroupRole.TEMP);
        users.forEach(user -> sendAlarmForNewMessage(user, messageDTO));
    }

    private void sendAlarmForNewMessage(User user, ChatRequest.MessageDTO messageDTO) {
        String content = "새로운 메시지: " + messageDTO.content();
        String redirectURL = "/chatting/" + messageDTO.chatRoomId();
        alarmService.sendAlarm(user.getId(), content, redirectURL, AlarmType.CHATTING);
    }

    private SimpleRabbitListenerEndpoint createRabbitListenerEndpoint(String listenerId, String queueName) {
        SimpleRabbitListenerEndpoint endpoint = new SimpleRabbitListenerEndpoint();
        endpoint.setId(listenerId);
        endpoint.setQueueNames(queueName);
        return endpoint;
    }
}