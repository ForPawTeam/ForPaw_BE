package com.hong.forapw.integration.rabbitmq;

import com.hong.forapw.domain.alarm.AlarmService;
import com.hong.forapw.domain.chat.ChatService;
import com.hong.forapw.domain.chat.MessageService;
import com.hong.forapw.domain.chat.model.MessageDTO;
import com.hong.forapw.domain.chat.repository.ChatRoomRepository;
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

import static com.hong.forapw.common.constants.GlobalConstants.CHAT_EXCHANGE;
import static com.hong.forapw.common.constants.GlobalConstants.ROOM_QUEUE_PREFIX;

@Component
@RequiredArgsConstructor
@Slf4j
public class RabbitMqService {

    private final ChatRoomRepository chatRoomRepository;
    private final RabbitListenerEndpointRegistry rabbitListenerEndpointRegistry;
    private final SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory;
    private final RabbitTemplate rabbitTemplate;
    private final AmqpAdmin amqpAdmin;
    private final MessageConverter converter;
    private final AlarmService alarmService;
    private final MessageService messageService;

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
            MessageDTO messageDTO = convertToMessageDTO(m);
            messageService.saveMessage(messageDTO);
            alarmService.sendAlarmToChatRoomUsers(messageDTO);
        });

        rabbitListenerEndpointRegistry.registerListenerContainer(endpoint, rabbitListenerContainerFactory, true);
    }

    public void sendChatMessageToRoom(Long chatRoomId, MessageDTO message) {
        String routingKey = ROOM_QUEUE_PREFIX + chatRoomId;
        rabbitTemplate.convertAndSend(CHAT_EXCHANGE, routingKey, message);
    }

    public void deleteQueue(String queueName) {
        amqpAdmin.deleteQueue(queueName);
    }

    private MessageDTO convertToMessageDTO(org.springframework.amqp.core.Message m) {
        return (MessageDTO) converter.fromMessage(m);
    }

    private SimpleRabbitListenerEndpoint createRabbitListenerEndpoint(String listenerId, String queueName) {
        SimpleRabbitListenerEndpoint endpoint = new SimpleRabbitListenerEndpoint();
        endpoint.setId(listenerId);
        endpoint.setQueueNames(queueName);
        return endpoint;
    }
}