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
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;

import static com.hong.forapw.integration.rabbitmq.RabbitMqConstants.CHAT_EXCHANGE;
import static com.hong.forapw.integration.rabbitmq.RabbitMqConstants.ROOM_QUEUE_PREFIX;

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

                    createAndBindQueueToExchange(CHAT_EXCHANGE, queueName);
                    registerMessageQueueListener(listenerId, queueName);
                });
    }

    public void initializeDeadLetterListener() {
        String dlqName = "chat.dead-letter.queue";
        String listenerId = "dlqListener";

        SimpleRabbitListenerEndpoint endpoint = createListenerEndpoint(listenerId, dlqName);
        endpoint.setMessageListener(message -> {
            String body = new String(message.getBody(), StandardCharsets.UTF_8);
            log.warn("DLQ 메시지 수신: {}", body);
        });

        rabbitListenerEndpointRegistry.registerListenerContainer(endpoint, rabbitListenerContainerFactory, true);
    }

    // 채팅방 큐를 생성하고 교환기에 바인딩
    public void createAndBindQueueToExchange(String exchangeName, String queueName) {
        DirectExchange exchange = new DirectExchange(exchangeName);
        amqpAdmin.declareExchange(exchange);

        // RabbitMQ는 공식적으로 'requeue=false'로 거부된 메시지를 큐 설정의 x-dead-letter-exchange 인자에 지정된 교환기로 라우팅
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

    public void registerMessageQueueListener(String listenerId, String queueName) {
        SimpleRabbitListenerEndpoint endpoint = createListenerEndpoint(listenerId, queueName);
        endpoint.setMessageListener(message -> { // 메시지 처리 로직 설정 (자동 ACK 모드)
            try {
                MessageDTO messageDTO = deserializeMessageToDTO(message);
                messageService.saveMessage(messageDTO);
                alarmService.sendAlarmToChatRoomUsers(messageDTO);
            } catch (Exception e) { // 예외를 그대로 던져서 Spring Retry가 재시도 처리하도록 함
                throw e;
            }
        });

        rabbitListenerEndpointRegistry.registerListenerContainer(endpoint, rabbitListenerContainerFactory, true);
    }

    @Retryable(backoff = @Backoff(delay = 1000, multiplier = 2))
    public void publishMessageToChatRoom(Long chatRoomId, MessageDTO message) {
        String routingKey = ROOM_QUEUE_PREFIX + chatRoomId;
        rabbitTemplate.convertAndSend(CHAT_EXCHANGE, routingKey, message);
    }

    @Recover
    public void logFailedMessagePublish(Exception e, Long chatRoomId, MessageDTO message) {
        log.error("메시지 발행 최종 실패: messageId={}, chatRoomId={}, error={}", message.messageId(), chatRoomId, e.getMessage(), e);
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