package com.hong.forapw.domain.alarm.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.hong.forapw.domain.alarm.constant.AlarmType;
import com.hong.forapw.domain.alarm.entity.Alarm;
import com.hong.forapw.domain.user.entity.User;

import java.time.LocalDateTime;

public record AlarmDTO(
        Long receiverId,
        String content,
        String redirectURL,
        @JsonProperty("meetDate")
        @JsonSerialize(using = LocalDateTimeSerializer.class)
        @JsonDeserialize(using = LocalDateTimeDeserializer.class)
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss.SSS")
        LocalDateTime date,
        AlarmType alarmType
) {
    public AlarmDTO(User user, String content, String redirectURL) {
        this(
                user.getId(),
                content,
                redirectURL,
                LocalDateTime.now(),
                AlarmType.CHATTING
        );
    }

    public Alarm toEntity(User receiver) {
        return Alarm.builder()
                .receiver(receiver)
                .content(content)
                .redirectURL(redirectURL)
                .alarmType(alarmType)
                .build();
    }
}
