package com.hong.forapw.domain.alarm.model.response;

import com.hong.forapw.domain.alarm.entity.Alarm;

import java.util.List;

public record FindAlarmListRes(List<AlarmDTO> alarms) {

    public static FindAlarmListRes fromEntities(List<Alarm> alarms){
        List<AlarmDTO> alarmDTOs = alarms.stream()
                .map(AlarmDTO::new)
                .toList();

        return new FindAlarmListRes(alarmDTOs);
    }
}