package com.hong.forapw.integration.rabbitmq;

import com.hong.forapw.domain.alarm.AlarmService;
import com.hong.forapw.domain.chat.MessageService;
import com.hong.forapw.domain.chat.model.MessageDTO;
import com.hong.forapw.domain.chat.repository.ChatRoomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerEndpoint;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.RabbitListenerEndpointRegistry;
import org.springframework.amqp.rabbit.listener.api.ChannelAwareMessageListener;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.Map;

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

        // 데드레터 교환기 설정이 포함된 큐 생성
        Queue queue = QueueBuilder.durable(queueName)
                .withArgument("x-dead-letter-exchange", "chat.dead-letter.exchange") // 메시지 처리 실패 시 이동할 exchange 지정
                .withArgument("x-dead-letter-routing-key", "chat.dead-letter.key") // 데드레터 exchange에서 사용할 라우팅 키 지정
                .withArgument("x-message-ttl", 30000) // 큐에서 30초 동안 처리되지 않은 메시지는 자동으로 데드레터로 이동
                .build();

        amqpAdmin.declareQueue(queue);

        Binding binding = BindingBuilder.bind(queue)
                .to(directExchange)
                .with(queueName); // routingKey는 큐 이름과 동일하게 사용
        amqpAdmin.declareBinding(binding);
    }

    public void registerChatListener(String listenerId, String queueName) {
        SimpleRabbitListenerEndpoint endpoint = createRabbitListenerEndpoint(listenerId, queueName);

        // 자동 ACK를 사용하므로, 직접 channel은 조작하지 않음
        endpoint.setMessageListener(message -> {
            try {
                MessageDTO messageDTO = convertToMessageDTO(message);
                messageService.saveMessage(messageDTO);
                alarmService.sendAlarmToChatRoomUsers(messageDTO);
            } catch (Exception e) { // 예외를 그대로 던져서 Spring Retry가 재시도 처리하도록 함
                throw e;
            }
        });

        // 리스너 컨테이너 등록
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