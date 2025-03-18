package com.hong.forapw.integration.rabbitmq;

import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.annotation.RabbitListenerConfigurer;
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.RabbitListenerEndpointRegistrar;
import org.springframework.amqp.rabbit.listener.RabbitListenerEndpointRegistry;
import org.springframework.amqp.rabbit.retry.RejectAndDontRequeueRecoverer;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.handler.annotation.support.DefaultMessageHandlerMethodFactory;

@Configuration
@RequiredArgsConstructor
public class RabbitMqConfig implements RabbitListenerConfigurer {

    private final ConnectionFactory connectionFactory;

    @Bean
    public RabbitAdmin rabbitAdmin() {
        RabbitAdmin rabbitAdmin = new RabbitAdmin(connectionFactory);
        rabbitAdmin.setAutoStartup(true);
        return rabbitAdmin;
    }

    @Bean
    public RabbitTemplate rabbitTemplate() {
        final RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jackson2JsonMessageConverter());
        return rabbitTemplate;
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory() {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMismatchedQueuesFatal(true);
        factory.setMessageConverter(jackson2JsonMessageConverter());

        // 자동 확인 모드 사용 -> 리스너 메서드가 예외 없이 완료되면 메시지 자동 확인
        factory.setAcknowledgeMode(AcknowledgeMode.AUTO);
        factory.setReceiveTimeout(30000L);
        factory.setConcurrentConsumers(3);
        factory.setMaxConcurrentConsumers(10);

        // 예외 발생 시 재시도 전략 설정
        factory.setAdviceChain(RetryInterceptorBuilder
                .stateless()               // 상태를 저장하지 않는 재시도(동일 스레드에서 재시도)
                .maxAttempts(5)            // 최대 5번 재시도
                .backOffOptions(1000, 2.0, 10000)  // 초기 1초, 2배씩 증가, 최대 10초 대기
                .recoverer(new RejectAndDontRequeueRecoverer())  // 모든 재시도 실패 후 메시지 거부
                .build());
        return factory;
    }

    @Bean
    public RabbitListenerEndpointRegistry rabbitListenerEndpointRegistry() {
        return new RabbitListenerEndpointRegistry();
    }

    @Bean
    public DefaultMessageHandlerMethodFactory messageHandlerMethodFactory() {
        DefaultMessageHandlerMethodFactory factory = new DefaultMessageHandlerMethodFactory();
        factory.setMessageConverter(consumerJackson2MessageConverter());
        return factory;
    }

    @Bean
    public Jackson2JsonMessageConverter jackson2JsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public MappingJackson2MessageConverter consumerJackson2MessageConverter() {
        return new MappingJackson2MessageConverter();
    }

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

    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable("chat.dead-letter.queue").build();
    }

    @Bean
    public Binding deadLetterBinding() {
        return BindingBuilder.bind(deadLetterQueue())
                .to(deadLetterExchange())
                .with("chat.dead-letter.key");
    }

    @Override
    public void configureRabbitListeners(RabbitListenerEndpointRegistrar registrar) {
        registrar.setContainerFactory(rabbitListenerContainerFactory());
        registrar.setEndpointRegistry(rabbitListenerEndpointRegistry());
        registrar.setMessageHandlerMethodFactory(messageHandlerMethodFactory());
    }
}