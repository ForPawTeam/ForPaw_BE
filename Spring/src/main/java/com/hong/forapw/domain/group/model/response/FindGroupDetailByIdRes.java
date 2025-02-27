package com.hong.forapw.domain.group.model.response;

import com.hong.forapw.domain.group.entity.Group;

import java.time.LocalDateTime;
import java.util.List;

public record FindGroupDetailByIdRes(
        String profileURL,
        String name,
        String description,
        List<NoticeDTO> notices,
        List<MeetingDTO> meetings,
        List<MemberDTO> members
) {

    public FindGroupDetailByIdRes(Group group, List<NoticeDTO> noticeDTOs, List<MeetingDTO> meetingDTOs, List<MemberDTO> memberDTOs) {
        this(
                group.getProfileURL(),
                group.getName(),
                group.getDescription(),
                noticeDTOs,
                meetingDTOs,
                memberDTOs
        );
    }

    public static List<MeetingDTO> fromMeetingResList(List<com.hong.forapw.domain.meeting.model.response.MeetingDTO> meetingResList) {
        return meetingResList.stream()
                .map(FindGroupDetailByIdRes.MeetingDTO::from)
                .toList();
    }

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

        public static MeetingDTO from(com.hong.forapw.domain.meeting.model.response.MeetingDTO meetingRes) {
            return new MeetingDTO(
                    meetingRes.id(),
                    meetingRes.name(),
                    meetingRes.meetDate(),
                    meetingRes.location(),
                    meetingRes.cost(),
                    meetingRes.participantNum(),
                    meetingRes.maxNum(),
                    meetingRes.profileURL(),
                    meetingRes.description(),
                    meetingRes.participants()
            );
        }
    }
}