package com.hong.forapw.config;

import com.hong.forapw.integration.rabbitmq.RabbitMqService;
import com.hong.forapw.domain.user.service.UserScheduler;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AppStartupListener implements ApplicationListener<ApplicationReadyEvent> {

    private final RabbitMqService brokerService;
    private final UserScheduler userScheduledTaskService;

    @Override
    public void onApplicationEvent(@NonNull ApplicationReadyEvent event) {
        brokerService.initializeAllChatRoomListeners();
        brokerService.initializeDeadLetterListener();
        userScheduledTaskService.initSuperAdmin();
    }
}
