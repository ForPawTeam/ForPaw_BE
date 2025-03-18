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

import static com.hong.forapw.common.constants.GlobalConstants.CHAT_EXCHANGE;
import static com.hong.forapw.common.constants.GlobalConstants.ROOM_QUEUE_PREFIX;

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
            if (failedMessage != null) {
                MessageDTO messageDTO = (MessageDTO) converter.fromMessage(failedMessage);

                // x-death 헤더 체크
                Map<String, Object> headers = failedMessage.getMessageProperties().getHeaders();
                List<?> xDeathHeader = (List<?>) headers.get("x-death");
                int retryCount = 0;

                if (xDeathHeader != null && !xDeathHeader.isEmpty()) {
                    Map<?, ?> xDeathProperties = (Map<?, ?>) xDeathHeader.get(0);
                    Number count = (Number) xDeathProperties.get("count");
                    retryCount = count != null ? count.intValue() : 0;
                }

                if (retryCount < 10) {
                    String routingKey = ROOM_QUEUE_PREFIX + messageDTO.chatRoomId();
                    rabbitTemplate.convertAndSend(CHAT_EXCHANGE, routingKey, messageDTO);
                }
            }
        } catch (Exception e) {
            log.error("데드레터 큐 처리 중 오류 발생", e);
        }
    }
}