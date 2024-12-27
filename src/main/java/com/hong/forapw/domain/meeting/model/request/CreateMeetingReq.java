package com.hong.forapw.domain.meeting.model.request;

import com.hong.forapw.domain.group.entity.Group;
import com.hong.forapw.domain.meeting.entity.Meeting;
import com.hong.forapw.domain.user.entity.User;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record CreateMeetingReq(
        @NotBlank(message = "정기모임의 이름을 입력해주세요.")
        String name,
        @NotNull(message = "모임 날짜를 입력해주세요.")
        LocalDateTime meetDate,
        @NotBlank(message = "모임 장소를 입력해주세요.")
        String location,
        @NotNull(message = "모임 비용을 입력해주세요.")
        Long cost,
        @NotNull(message = "최대 인원수를 입력해주세요.")
        Integer maxNum,
        @NotBlank(message = "모임의 설명을 입력해주세요.")
        String description,
        @NotBlank
        String profileURL
) {
    public Meeting toEntity(Group group, User creator) {
        return Meeting.builder()
                .group(group)
                .creator(creator)
                .name(name)
                .meetDate(meetDate)
                .location(location)
                .cost(cost)
                .maxNum(maxNum)
                .description(description)
                .profileURL(profileURL)
                .build();
    }
}
