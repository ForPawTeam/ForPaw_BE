package com.hong.forapw.domain.alarm.model.response;

import com.hong.forapw.domain.alarm.entity.Alarm;

import java.time.LocalDateTime;
import java.util.List;

public record FindAlarmListRes(List<AlarmDTO> alarms) {

    public record AlarmDTO(Long id,
                           String content,
                           String redirectURL,
                           LocalDateTime date,
                           boolean isRead) {

        public AlarmDTO(Alarm alarm) {
            this(
                    alarm.getId(),
                    alarm.getContent(),
                    alarm.getRedirectURL(),
                    alarm.getCreatedDate(),
                    alarm.getIsRead());
        }

        public AlarmDTO(Alarm alarm, boolean isRead) {
            this(
                    alarm.getId(),
                    alarm.getContent(),
                    alarm.getRedirectURL(),
                    alarm.getCreatedDate(),
                    isRead);
        }
    }
}
