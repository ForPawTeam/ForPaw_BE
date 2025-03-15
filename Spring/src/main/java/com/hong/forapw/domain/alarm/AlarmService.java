package com.hong.forapw.domain.alarm;

import com.hong.forapw.common.exceptions.CustomException;
import com.hong.forapw.common.exceptions.ExceptionCode;
import com.hong.forapw.domain.alarm.entity.Alarm;
import com.hong.forapw.domain.alarm.constant.AlarmType;
import com.hong.forapw.domain.alarm.model.response.AlarmDTO;
import com.hong.forapw.domain.alarm.model.response.FindAlarmListRes;
import com.hong.forapw.domain.alarm.repository.AlarmRepository;
import com.hong.forapw.domain.alarm.repository.EmitterRepository;
import com.hong.forapw.domain.chat.model.MessageDTO;
import com.hong.forapw.domain.chat.repository.ChatRoomRepository;
import com.hong.forapw.domain.group.constant.GroupRole;
import com.hong.forapw.domain.user.entity.User;
import com.hong.forapw.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AlarmService {

    private final AlarmRepository alarmRepository;
    private final UserRepository userRepository;
    private final EmitterRepository emitterRepository;
    private final ChatRoomRepository chatRoomRepository;

    private static final Long DEFAULT_TIMEOUT = 60L * 1000 * 60;
    private static final String SSE_EVENT_NAME = "sse";

    @Transactional
    @Scheduled(cron = "0 30 1 * * *")
    public void cleanUpAlarms() {
        LocalDateTime oneWeekAgo = LocalDateTime.now().minusWeeks(1);
        alarmRepository.deleteReadAlarmBefore(oneWeekAgo);

        LocalDateTime oneMonthAgo = LocalDateTime.now().minusMonths(1);
        alarmRepository.deleteNotReadAlarmBefore(oneMonthAgo);
    }

    @Transactional
    public SseEmitter connectToSseForAlarms(String userId) {
        String emitterId = createTimestampedId(userId);
        SseEmitter emitter = emitterRepository.save(emitterId, new SseEmitter(DEFAULT_TIMEOUT));

        handleEmitterCompletion(emitter, emitterId);
        sendKeepAliveEvent(userId, emitter, emitterId);

        return emitter;
    }

    @Transactional
    public void sendAlarm(Long receiverId, String content, String redirectURL, AlarmType alarmType) {
        User receiver = userRepository.getReferenceById(receiverId);
        Alarm alarm = Alarm.builder()
                .receiver(receiver)
                .content(content)
                .redirectURL(redirectURL)
                .alarmType(alarmType)
                .build();

        alarmRepository.save(alarm);
        sendAlarmViaSSE(alarm);
    }

    @Transactional
    public void sendAlarmToChatRoomUsers(MessageDTO messageDTO) {
        List<User> users = chatRoomRepository.findUsersByChatRoomIdExcludingRole(messageDTO.chatRoomId(), GroupRole.TEMP);

        users.forEach(user -> {
            String content = "새로운 메시지: " + messageDTO.content();
            String redirectURL = "/chatting/" + messageDTO.chatRoomId();
            internalSendAlarm(user.getId(), content, redirectURL);
        });
    }

    public FindAlarmListRes findAlarms(Long userId) {
        List<Alarm> alarms = alarmRepository.findByReceiverId(userId);
        if (alarms.isEmpty()) {
            throw new CustomException(ExceptionCode.ALARM_LIST_EMPTY);
        }

        return FindAlarmListRes.fromEntities(alarms);
    }

    @Transactional
    public void updateAlarmAsRead(Long alarmId, Long userId) {
        Alarm alarm = alarmRepository.findById(alarmId)
                .orElseThrow(() -> new CustomException(ExceptionCode.ALARM_NOT_FOUND));

        validateAlarmAuthorization(userId, alarm);
        alarm.updateIsRead(true, LocalDateTime.now());
    }

    // 503 에러를 방지하기 위해 더미 이벤트 전송
    private void sendKeepAliveEvent(String userId, SseEmitter emitter, String emitterId) {
        String eventId = createTimestampedId(userId);
        emitAlarmEvent(emitter, eventId, emitterId, "ForPaw");
    }

    private void sendAlarmViaSSE(Alarm alarm) {
        String receiverId = alarm.getReceiverId().toString();
        String eventId = createTimestampedId(receiverId);

        Map<String, SseEmitter> emitters = emitterRepository.findEmittersByMemberIdPrefix(receiverId);
        emitters.forEach((key, emitter) -> emitAlarmToEmitter(emitter, key, alarm, eventId));
    }

    private void emitAlarmToEmitter(SseEmitter emitter, String emitterId, Alarm alarm, String eventId) {
        AlarmDTO alarmDTO = new AlarmDTO(alarm, false);
        emitAlarmEvent(emitter, eventId, emitterId, alarmDTO);
    }

    private void handleEmitterCompletion(SseEmitter emitter, String emitterId) {
        emitter.onCompletion(() -> emitterRepository.deleteById(emitterId));
        emitter.onTimeout(() -> emitterRepository.deleteById(emitterId));
    }

    private void validateAlarmAuthorization(Long userId, Alarm alarm) {
        if (!alarm.getReceiverId().equals(userId)) {
            throw new CustomException(ExceptionCode.UNAUTHORIZED_ACCESS);
        }
    }

    private void emitAlarmEvent(SseEmitter emitter, String eventId, String emitterId, Object data) {
        try {
            emitter.send(SseEmitter.event()
                    .id(eventId)
                    .name(SSE_EVENT_NAME)
                    .data(data)
            );
        } catch (IOException e) {
            log.error("SSE 이벤트 전송 실패, emitterId: {}", emitterId, e);
            emitterRepository.deleteById(emitterId);
        }
    }

    private String createTimestampedId(String userId) {
        return userId + "_" + System.currentTimeMillis();
    }

    private void internalSendAlarm(Long receiverId, String content, String redirectURL) {
        User receiver = userRepository.getReferenceById(receiverId);
        Alarm alarm = Alarm.builder()
                .receiver(receiver)
                .content(content)
                .redirectURL(redirectURL)
                .alarmType(AlarmType.CHATTING)
                .build();

        alarmRepository.save(alarm);
        sendAlarmViaSSE(alarm);
    }
}