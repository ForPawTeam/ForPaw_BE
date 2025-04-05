package com.hong.forapw.integration.rabbitmq;

import com.hong.forapw.domain.alarm.AlarmService;
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

    // 애플리케이션 시작 시 모든 채팅방의 메시지 리스너를 초기화
    @Transactional
    public void initializeAllChatRoomListeners() {
        chatRoomRepository.findAll()
                .forEach(chatRoom -> {
                    String queueName = ROOM_QUEUE_PREFIX + chatRoom.getId();
                    String listenerId = ROOM_QUEUE_PREFIX + chatRoom.getId();

                    bindDirectExchangeToQueue(CHAT_EXCHANGE, queueName);
                    createMessageConsumerForQueue(listenerId, queueName);
                });
    }

    // 채팅방 큐를 생성하고 교환기에 바인딩
    public void bindDirectExchangeToQueue(String exchangeName, String queueName) {
        DirectExchange exchange = new DirectExchange(exchangeName);

        // 데드레터 교환기 설정이 포함된 큐 생성
        Queue queue = QueueBuilder.durable(queueName)
                .withArgument("x-dead-letter-exchange", "chat.dead-letter.exchange") // 메시지 처리 실패 시 이동할 exchange 지정
                .withArgument("x-dead-letter-routing-key", "chat.dead-letter.key") // 데드레터 exchange에서 사용할 라우팅 키 지정
                .withArgument("x-message-ttl", 30000) // 큐에서 30초 동안 처리되지 않은 메시지는 자동으로 데드레터로 이동
                .build();
        amqpAdmin.declareQueue(queue);

        // 큐와 교환기를 라우팅 키로 바인딩 (라우팅 키는 큐 이름과 동일)
        Binding binding = BindingBuilder.bind(queue)
                .to(exchange)
                .with(queueName);
        amqpAdmin.declareBinding(binding);
    }

    // 지정된 큐에 대한 메시지 소비자를 생성하고 등록
    public void createMessageConsumerForQueue(String listenerId, String queueName) {
        SimpleRabbitListenerEndpoint endpoint = createListenerEndpoint(listenerId, queueName);

        // 메시지 처리 로직 설정 (자동 ACK 모드)
        endpoint.setMessageListener(message -> {
            try {
                MessageDTO messageDTO = deserializeMessageToDTO(message);
                messageService.saveMessage(messageDTO);
                alarmService.sendAlarmToChatRoomUsers(messageDTO);
            } catch (Exception e) { // 예외를 그대로 던져서 Spring Retry가 재시도 처리하도록 함
                throw e;
            }
        });

        // 리스너 컨테이너 레지스트리에 등록 (동적 리스너 생성)
        rabbitListenerEndpointRegistry.registerListenerContainer(endpoint, rabbitListenerContainerFactory, true);
    }

    public void publishMessageToChatRoom(Long chatRoomId, MessageDTO message) {
        String routingKey = ROOM_QUEUE_PREFIX + chatRoomId;
        rabbitTemplate.convertAndSend(CHAT_EXCHANGE, routingKey, message);
    }

    public void deleteQueue(String queueName) {
        amqpAdmin.deleteQueue(queueName);
    }

    // AMQP 메시지를 MessageDTO 객체로 변환
    private MessageDTO deserializeMessageToDTO(org.springframework.amqp.core.Message amqpMessage) {
        return (MessageDTO) converter.fromMessage(amqpMessage);
    }

    // 리스너 엔드포인트 생성
    private SimpleRabbitListenerEndpoint createListenerEndpoint(String listenerId, String queueName) {
        SimpleRabbitListenerEndpoint endpoint = new SimpleRabbitListenerEndpoint();
        endpoint.setId(listenerId);
        endpoint.setQueueNames(queueName);
        return endpoint;
    }
}