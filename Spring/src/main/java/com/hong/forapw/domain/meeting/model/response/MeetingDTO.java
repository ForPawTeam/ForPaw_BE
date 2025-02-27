package com.hong.forapw.domain.meeting.model.response;

import com.hong.forapw.domain.meeting.entity.Meeting;

import java.time.LocalDateTime;
import java.util.List;

public record MeetingDTO(
        Long id,
        String name,
        LocalDateTime meetDate,
        String location,
        Long cost,
        Long participantNum,
        Integer maxNum,
        String profileURL,
        String description,
        List<String> participants) {

    public static MeetingDTO fromEntity(Meeting meeting, List<String> participants) {
        return new MeetingDTO(
                meeting.getId(),
                meeting.getName(),
                meeting.getMeetDate(),
                meeting.getLocation(),
                meeting.getCost(),
                meeting.getParticipantNum(),
                meeting.getMaxNum(),
                meeting.getProfileURL(),
                meeting.getDescription(),
                participants);
    }
}
