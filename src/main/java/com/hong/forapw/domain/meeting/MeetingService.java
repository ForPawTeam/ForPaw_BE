package com.hong.forapw.domain.meeting;

import com.hong.forapw.domain.alarm.AlarmService;
import com.hong.forapw.domain.meeting.model.query.MeetingUserProfileDTO;
import com.hong.forapw.common.exceptions.CustomException;
import com.hong.forapw.common.exceptions.ExceptionCode;
import com.hong.forapw.domain.alarm.constant.AlarmType;
import com.hong.forapw.domain.group.entity.Group;
import com.hong.forapw.domain.meeting.entity.Meeting;
import com.hong.forapw.domain.meeting.entity.MeetingUser;
import com.hong.forapw.domain.meeting.model.request.CreateMeetingReq;
import com.hong.forapw.domain.meeting.model.request.UpdateMeetingReq;
import com.hong.forapw.domain.meeting.model.response.CreateMeetingRes;
import com.hong.forapw.domain.meeting.model.response.FindMeetingByIdRes;
import com.hong.forapw.domain.meeting.model.response.MeetingDTO;
import com.hong.forapw.domain.meeting.repository.MeetingRepository;
import com.hong.forapw.domain.user.entity.User;
import com.hong.forapw.domain.user.repository.UserRepository;
import com.hong.forapw.domain.group.repository.GroupRepository;
import com.hong.forapw.domain.group.repository.GroupUserRepository;
import com.hong.forapw.domain.meeting.repository.MeetingUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MeetingService {

    private final MeetingRepository meetingRepository;
    private final UserRepository userRepository;
    private final MeetingUserRepository meetingUserRepository;
    private final GroupUserRepository groupUserRepository;
    private final GroupRepository groupRepository;
    private final AlarmService alarmService;
    private final MeetingValidator validator;

    @Transactional
    public CreateMeetingRes createMeeting(CreateMeetingReq request, Long groupId, Long userId) {
        validator.validateGroupExists(groupId);
        validator.validateMeetingNameNotDuplicate(request, groupId);

        User creator = userRepository.findNonWithdrawnById(userId)
                .orElseThrow(() -> new CustomException(ExceptionCode.USER_NOT_FOUND));
        validator.validateGroupAdminAuthorization(creator, groupId);

        Group group = groupRepository.getReferenceById(groupId);
        Meeting meeting = request.toEntity(group, creator);
        addMeetingCreatorToParticipants(creator, meeting);

        notifyGroupMembersAboutNewMeeting(groupId, userId, request.name());

        return new CreateMeetingRes(meeting.getId());
    }

    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void deleteExpiredMeetings() {
        LocalDateTime now = LocalDateTime.now();
        List<Meeting> expiredMeetings = meetingRepository.findByMeetDateBefore(now);

        expiredMeetings.forEach(meeting -> {
            meetingUserRepository.deleteAllByMeetingId(meeting.getId());
            meetingRepository.deleteById(meeting.getId());
        });
    }

    @Transactional
    public void updateMeeting(UpdateMeetingReq request, Long groupId, Long meetingId, Long userId) {
        validator.validateMeetingExists(meetingId);

        User groupAdmin = userRepository.getReferenceById(userId);
        validator.validateGroupAdminAuthorization(groupAdmin, groupId);

        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new CustomException(ExceptionCode.MEETING_NOT_FOUND));

        meeting.updateMeeting(request.name(), request.meetDate(), request.location(), request.cost(), request.maxNum(),
                request.description(), request.profileURL());
    }

    @Transactional
    public void joinMeeting(Long groupId, Long meetingId, Long userId) {
        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new CustomException(ExceptionCode.MEETING_NOT_FOUND));

        validator.validateGroupMembership(groupId, userId);
        validator.validateNotAlreadyMeetingParticipant(meetingId, userId);

        User joiner = userRepository.getReferenceById(userId);
        addParticipantToMeeting(joiner, meeting);
    }

    @Transactional
    public void withdrawMeeting(Long groupId, Long meetingId, Long userId) {
        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new CustomException(ExceptionCode.MEETING_NOT_FOUND));

        validator.validateGroupMembership(groupId, userId);
        validator.validateMeetingParticipation(meetingId, userId);

        meetingUserRepository.deleteByMeetingIdAndUserId(meetingId, userId);
        meeting.decrementParticipantCount();
    }

    @Transactional
    public void deleteMeeting(Long groupId, Long meetingId, Long userId) {
        validator.validateMeetingExists(meetingId);

        User groupAdmin = userRepository.getReferenceById(userId);
        validator.validateGroupAdminAuthorization(groupAdmin, groupId);

        meetingUserRepository.deleteAllByMeetingId(meetingId);
        meetingRepository.deleteById(meetingId);
    }

    public List<MeetingDTO> findMeetings(Long groupId, Pageable pageable) {
        Page<Meeting> meetingPage = meetingRepository.findByGroupId(groupId, pageable);
        Map<Long, List<String>> meetingUserProfiles = getMeetingUserProfilesByGroupId(groupId);

        return meetingPage.getContent().stream()
                .map(meeting -> MeetingDTO.fromEntity(meeting, meetingUserProfiles.getOrDefault(meeting.getId(), Collections.emptyList())))
                .toList();
    }

    public FindMeetingByIdRes findMeetingById(Long meetingId, Long groupId, Long userId) {
        validator.validateGroupExists(groupId);
        validator.validateGroupMembership(groupId, userId);

        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new CustomException(ExceptionCode.MEETING_NOT_FOUND));

        List<User> participants = meetingUserRepository.findUserByMeetingId(meeting.getId());
        List<FindMeetingByIdRes.ParticipantDTO> participantDTOs = FindMeetingByIdRes.ParticipantDTO.fromEntity(participants);

        return FindMeetingByIdRes.fromEntity(meeting, participantDTOs);
    }

    private void addMeetingCreatorToParticipants(User creator, Meeting meeting) {
        MeetingUser meetingUser = MeetingUser.builder()
                .user(creator)
                .build();
        meeting.addMeetingUser(meetingUser); // cascade에 의해 meetingUser도 자동으로 저장됨
        meetingRepository.save(meeting);

        meeting.incrementParticipantCount();
    }

    private void notifyGroupMembersAboutNewMeeting(Long groupId, Long creatorId, String meetingName) {
        List<User> groupMembers = groupUserRepository.findUserByGroupIdWithoutMe(groupId, creatorId);
        for (User member : groupMembers) {
            String content = "새로운 정기 모임: " + meetingName;
            String redirectURL = "/volunteer/" + groupId;
            alarmService.sendAlarm(member.getId(), content, redirectURL, AlarmType.NEW_MEETING);
        }
    }

    private void addParticipantToMeeting(User joiner, Meeting meeting) {
        MeetingUser meetingUser = MeetingUser.builder()
                .user(joiner)
                .build();
        meeting.addMeetingUser(meetingUser);
        meetingUserRepository.save(meetingUser);

        meeting.incrementParticipantCount();
    }

    private Map<Long, List<String>> getMeetingUserProfilesByGroupId(Long groupId) {
        // <Long meetingId, List<String> userProfiles>
        return meetingUserRepository.findMeetingUsersByGroupId(groupId).stream()
                .collect(Collectors.groupingBy(
                        MeetingUserProfileDTO::meetingId,
                        Collectors.mapping(MeetingUserProfileDTO::profileURL, Collectors.toList())
                ));
    }
}