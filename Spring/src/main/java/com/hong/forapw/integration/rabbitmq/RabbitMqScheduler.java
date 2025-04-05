package com.hong.forapw.integration.rabbitmq;

import com.hong.forapw.domain.chat.model.MessageDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

import static com.hong.forapw.integration.rabbitmq.RabbitMqConstants.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class RabbitMqScheduler {

    private final RabbitTemplate rabbitTemplate;
    private final MessageConverter converter;

    @Scheduled(fixedDelay = 60000)
    public void processDeadLetterQueue() {
        try {
            Message failedMessage = rabbitTemplate.receive("chat.dead-letter.queue");

            if (failedMessage != null && extractRetryCount(failedMessage) < MAX_RETRY_COUNT) {
                MessageDTO messageDTO = deserializeMessage(failedMessage);
                resendMessageToOriginalQueue(messageDTO);
            }
        } catch (Exception e) {
            log.error("데드레터 큐 처리 중 오류 발생", e);
        }
    }

    private MessageDTO deserializeMessage(Message message) {
        return (MessageDTO) converter.fromMessage(message);
    }

    private int extractRetryCount(Message message) {
        Map<String, Object> headers = message.getMessageProperties().getHeaders();
        List<?> xDeathHeader = (List<?>) headers.get(X_DEATH_HEADER);

        if (xDeathHeader == null || xDeathHeader.isEmpty())
            return 0;

        Map<?, ?> xDeathProperties = (Map<?, ?>) xDeathHeader.get(0);
        Number count = (Number) xDeathProperties.get(COUNT_PROPERTY);
        return count != null ? count.intValue() : 0;
    }

    private void resendMessageToOriginalQueue(MessageDTO messageDTO) {
        String routingKey = ROOM_QUEUE_PREFIX + messageDTO.chatRoomId();
        rabbitTemplate.convertAndSend(CHAT_EXCHANGE, routingKey, messageDTO);
    }
}