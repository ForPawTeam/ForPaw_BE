package com.hong.forapw.domain.meeting.model.response;

import com.hong.forapw.domain.meeting.entity.Meeting;
import com.hong.forapw.domain.user.entity.User;

import java.time.LocalDateTime;
import java.util.List;

public record FindMeetingByIdRes(
        Long id,
        String name,
        LocalDateTime meetDate,
        String location,
        Long cost,
        Long participantNum,
        Integer maxNum,
        String organizer,
        String profileURL,
        String description,
        List<ParticipantDTO> participants
) {

    public static FindMeetingByIdRes fromEntity(Meeting meeting, List<ParticipantDTO> participantDTOS) {
        return new FindMeetingByIdRes(
                meeting.getId(),
                meeting.getName(),
                meeting.getMeetDate(),
                meeting.getLocation(),
                meeting.getCost(),
                meeting.getParticipantNum(),
                meeting.getMaxNum(),
                meeting.getCreatorNickName(),
                meeting.getProfileURL(),
                meeting.getDescription(),
                participantDTOS);
    }

    public record ParticipantDTO(
            String profileURL,
            String nickName) {

        public static List<ParticipantDTO> fromEntity(List<User> participants) {
            return participants.stream()
                    .map(user -> new ParticipantDTO(user.getProfileURL(), user.getNickname()))
                    .toList();
        }
    }
}