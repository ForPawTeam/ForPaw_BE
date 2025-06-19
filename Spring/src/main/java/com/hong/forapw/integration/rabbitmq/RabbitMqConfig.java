package com.hong.forapw.integration.rabbitmq;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.RabbitListenerEndpointRegistry;
import org.springframework.amqp.rabbit.retry.RejectAndDontRequeueRecoverer;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class RabbitMqConfig {

    private final ConnectionFactory connectionFactory;

    //==== 기본 RabbitMQ 컴포넌트 ====//

    // RabbitMQ 관리 작업(Queue, Exchange 생성 등)을 위한 컴포넌트
    @Bean
    public RabbitAdmin rabbitAdmin() {
        RabbitAdmin rabbitAdmin = new RabbitAdmin(connectionFactory);
        rabbitAdmin.setAutoStartup(true);
        return rabbitAdmin;
    }

    // 메시지 발행을 위한 컴포넌트 for Producer
    @Bean
    public RabbitTemplate rabbitTemplate() {
        final RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jackson2JsonMessageConverter());
        rabbitTemplate.setReplyTimeout(5000);
        return rabbitTemplate;
    }

    //==== 리스너 컨테이너 및 관련 팩토리 ====//

    // 동적으로 생성되는 리스너를 관리하는 컴포넌트
    @Bean
    public RabbitListenerEndpointRegistry rabbitListenerEndpointRegistry() {
        return new RabbitListenerEndpointRegistry();
    }

    // 메시지 리스너 컨테이너 설정을 위한 컴포넌트 for Consumer
    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory() {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);

        factory.setMismatchedQueuesFatal(true); // 큐 불일치 오류 시 실패 처리
        factory.setMessageConverter(jackson2JsonMessageConverter());

        // 메시지 확인 모드 -> 메시지 정상 완료 시 자동 확인
        factory.setAcknowledgeMode(AcknowledgeMode.AUTO);
        factory.setReceiveTimeout(30000L);

        // 동시 소비자 수 설정 -> 부하에 따라 3-10개 사이 자동 조정
        factory.setConcurrentConsumers(3);
        factory.setMaxConcurrentConsumers(10);

        // 예외 발생 시 재시도 전략
        factory.setAdviceChain(RetryInterceptorBuilder
                .stateless()  // 상태를 저장하지 않는 재시도(동일 스레드에서 재시도)
                .maxAttempts(5)
                .backOffOptions(1000, 2.0, 10000)
                .recoverer(new RejectAndDontRequeueRecoverer())  // 모든 재시도 실패 후 메시지 거부 및 데드레터 큐로 자동 라우팅
                .build());

        return factory;
    }

    //==== 메시지 Converters ====//

    // JSON 메시지 컨버터
    @Bean
    public Jackson2JsonMessageConverter jackson2JsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    //==== Exchanges, Queues, Bindings 설정 ====//

    // 채팅을 위한 Exchange
    @Bean
    DirectExchange chatExchange() {
        return new DirectExchange("chat.exchange");
    }

    // Dead Letter를 위한 Exchange
    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange("chat.dead-letter.exchange");
    }

    // Dead Letter를 위한 Queue
    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable("chat.dead-letter.queue").build();
    }

    // Dead Letter Exchange와 Queue 연결
    @Bean
    public Binding deadLetterBinding() {
        return BindingBuilder.bind(deadLetterQueue())
                .to(deadLetterExchange())
                .with("chat.dead-letter.key");
    }
}