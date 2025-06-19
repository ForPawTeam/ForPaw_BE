package com.hong.forapw.domain.group.service;

import com.hong.forapw.domain.alarm.AlarmService;
import com.hong.forapw.domain.group.GroupValidator;
import com.hong.forapw.domain.group.entity.Group;
import com.hong.forapw.domain.group.constant.GroupRole;
import com.hong.forapw.domain.group.entity.GroupUser;
import com.hong.forapw.domain.group.model.request.*;
import com.hong.forapw.domain.group.model.response.*;
import com.hong.forapw.domain.group.repository.FavoriteGroupRepository;
import com.hong.forapw.domain.group.repository.GroupRepository;
import com.hong.forapw.domain.group.repository.GroupUserRepository;
import com.hong.forapw.domain.like.common.Like;
import com.hong.forapw.domain.meeting.model.response.MeetingDTO;
import com.hong.forapw.domain.post.repository.*;
import com.hong.forapw.domain.meeting.repository.MeetingRepository;
import com.hong.forapw.domain.meeting.MeetingService;
import com.hong.forapw.domain.meeting.repository.MeetingUserRepository;
import com.hong.forapw.common.exceptions.CustomException;
import com.hong.forapw.common.exceptions.ExceptionCode;
import com.hong.forapw.domain.alarm.constant.AlarmType;
import com.hong.forapw.domain.chat.entity.ChatRoom;
import com.hong.forapw.domain.chat.entity.ChatUser;
import com.hong.forapw.domain.post.entity.Post;
import com.hong.forapw.domain.region.constant.District;
import com.hong.forapw.domain.region.constant.Province;
import com.hong.forapw.domain.user.entity.User;
import com.hong.forapw.domain.chat.repository.ChatRoomRepository;
import com.hong.forapw.domain.chat.repository.ChatUserRepository;
import com.hong.forapw.domain.user.repository.UserRepository;
import com.hong.forapw.integration.rabbitmq.RabbitMqService;
import com.hong.forapw.domain.like.LikeService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

import static com.hong.forapw.common.utils.PaginationUtils.*;
import static com.hong.forapw.integration.rabbitmq.RabbitMqConstants.CHAT_EXCHANGE;
import static com.hong.forapw.integration.rabbitmq.RabbitMqConstants.ROOM_QUEUE_PREFIX;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GroupService {

    private final GroupRepository groupRepository;
    private final GroupUserRepository groupUserRepository;
    private final FavoriteGroupRepository favoriteGroupRepository;
    private final MeetingRepository meetingRepository;
    private final MeetingUserRepository meetingUserRepository;
    private final PostRepository postRepository;
    private final PostLikeRepository postLikeRepository;
    private final PostImageRepository postImageRepository;
    private final CommentRepository commentRepository;
    private final CommentLikeRepository commentLikeRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final ChatUserRepository chatUserRepository;
    private final UserRepository userRepository;
    private final RabbitMqService rabbitMqService;
    private final AlarmService alarmService;
    private final LikeService likeService;
    private final MeetingService meetingService;
    private final GroupCacheService groupCacheService;
    private final GroupValidator validator;

    private static final Province DEFAULT_PROVINCE = Province.DAEGU;
    private static final int DEFAULT_RECOMMENDATION_LIMIT = 5;

    @Transactional
    public CreateGroupRes createGroup(CreateGroupReq request, Long creatorId) {
        validator.validateGroupNameNotDuplicate(request.name());

        Group group = request.toEntity();
        groupRepository.save(group);

        User groupOwner = addGroupOwner(group, creatorId);
        ChatRoom chatRoom = addChatRoom(group);
        addChatUserToRoom(chatRoom, groupOwner);

        configureRabbitMQForChatRoom(chatRoom);

        return new CreateGroupRes(group.getId());
    }

    // 클라이언트단에서 수정할 때 사용하는 API
    public FindGroupByIdRes findGroupById(Long groupId, Long groupAdminId) {
        validator.validateGroupAdminAuthorization(groupAdminId, groupId);

        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new CustomException(ExceptionCode.GROUP_NOT_FOUND));

        return new FindGroupByIdRes(group);
    }

    @Transactional
    public void updateGroup(UpdateGroupReq request, Long groupId, Long groupAdminId) {
        validator.validateGroupAdminAuthorization(groupAdminId, groupId);

        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new CustomException(ExceptionCode.GROUP_NOT_FOUND));

        validator.validateGroupNameNotDuplicateExcludingId(groupId, request.name());
        updateGroupChatRoomName(groupId, request.name());

        updateGroupInfo(request, group);
    }

    public FindGroupMemberListRes findGroupMembers(Long groupAdminId, Long groupId) {
        validator.validateGroupAdminAuthorization(groupAdminId, groupId);

        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new CustomException(ExceptionCode.GROUP_NOT_FOUND));

        List<FindGroupMemberListRes.MemberDetailDTO> memberDetails = getMemberDetails(groupId);

        return new FindGroupMemberListRes(group.getParticipantNum(), group.getMaxNum(), memberDetails);
    }

    public FindAllGroupListRes findGroups(Long userId) {
        Province province = determineProvince(userId);

        List<Long> likedGroupIdList = getLikedGroupIds(userId);
        List<RecommendGroupDTO> recommendGroupDTOS = getRecommendGroups(userId, province, likedGroupIdList);
        List<NewGroupDTO> newGroupDTOS = getNewGroups(userId, province, DEFAULT_PAGEABLE);
        List<MyGroupDTO> myGroupDTOS = getMyGroups(userId, likedGroupIdList, DEFAULT_PAGEABLE);

        return new FindAllGroupListRes(recommendGroupDTOS, newGroupDTOS, myGroupDTOS);
    }

    public FindLocalAndNewGroupListRes findLocalAndNewGroups(Long userId, Province province, District district, List<Long> likedGroupIds, Pageable pageable) {
        List<LocalGroupDTO> localGroupDTOS = getLocalGroups(userId, province, district, likedGroupIds, pageable);
        List<NewGroupDTO> newGroupDTOS = getNewGroups(userId, province, pageable);

        return new FindLocalAndNewGroupListRes(localGroupDTOS, newGroupDTOS);
    }

    public List<LocalGroupDTO> getLocalGroups(Long userId, Province province, District district, List<Long> likedGroupIds, Pageable pageable) {
        List<Group> localGroups = groupRepository.findByProvinceAndDistrictWithoutMyGroup(province, district, userId, GroupRole.TEMP, pageable).getContent();
        List<Long> groupIds = localGroups.stream()
                .map(Group::getId)
                .toList();

        Map<Long, Long> likeCountMap = likeService.getLikeCounts(groupIds, Like.GROUP);
        return LocalGroupDTO.fromEntities(localGroups, likeCountMap, likedGroupIds);
    }

    public FindGroupDetailByIdRes findGroupDetailById(Long userId, Long groupId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new CustomException(ExceptionCode.GROUP_NOT_FOUND));

        List<MeetingDTO> meetingDTOs = meetingService.findMeetings(groupId, DEFAULT_PAGEABLE);
        List<FindGroupDetailByIdRes.MeetingDTO> convertedMeetingDTOs = FindGroupDetailByIdRes.fromMeetingResList(meetingDTOs);

        List<NoticeDTO> noticeDTOs = getNotices(userId, groupId, DEFAULT_PAGEABLE);
        List<MemberDTO> memberDTOs = getMembers(groupId);

        return new FindGroupDetailByIdRes(group, noticeDTOs, convertedMeetingDTOs, memberDTOs);
    }

    @Transactional
    public void joinGroup(JoinGroupReq request, Long userId, Long groupId) {
        Group group = groupRepository.findById(groupId).orElseThrow(
                () -> new CustomException(ExceptionCode.GROUP_NOT_FOUND)
        );

        group.validateCapacity();
        validator.validateNotAlreadyGroupMember(groupId, userId);

        User applicant = userRepository.getReferenceById(userId);
        addTemporaryGroupMember(applicant, group, request.greeting());
    }

    @Transactional
    public void withdrawGroup(Long userId, Long groupId) {
        validator.validateGroupMembership(groupId, userId);

        groupUserRepository.deleteByGroupIdAndUserId(groupId, userId);
        chatUserRepository.deleteByGroupIdAndUserId(groupId, userId);
        meetingUserRepository.deleteByGroupIdAndUserId(groupId, userId);

        groupRepository.decrementParticipantNum(groupId);
        meetingRepository.decrementParticipantCountForUserMeetings(groupId, userId);
    }

    @Transactional
    public void expelGroupMember(Long groupAdminId, Long memberId, Long groupId) {
        validator.validateGroupAdminAuthorization(groupAdminId, groupId);

        groupUserRepository.deleteByGroupIdAndUserId(groupId, memberId);
        chatUserRepository.deleteByGroupIdAndUserId(groupId, memberId);
        meetingUserRepository.deleteByGroupIdAndUserId(groupId, memberId);

        groupRepository.decrementParticipantNum(groupId);
        meetingRepository.decrementParticipantCountForUserMeetings(groupId, memberId);
    }

    public FindApplicantListRes findApplicants(Long groupAdminId, Long groupId) {
        validator.validateGroupAdminAuthorization(groupAdminId, groupId);

        List<GroupUser> applicants = groupUserRepository.findByGroupRole(groupId, GroupRole.TEMP);
        List<FindApplicantListRes.ApplicantDTO> applicantDTOS = FindApplicantListRes.ApplicantDTO.toDTOs(applicants);

        return new FindApplicantListRes(applicantDTOS);
    }

    @Transactional
    public void approveJoin(Long groupAdminId, Long applicantId, Long groupId) {
        validator.validateGroupAdminAuthorization(groupAdminId, groupId);

        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new CustomException(ExceptionCode.GROUP_NOT_FOUND));

        GroupUser groupUser = getGroupUser(groupId, applicantId);
        groupUser.validateNotAlreadyMember();

        approveMembership(groupUser, group);
        notifyApplicant(applicantId, groupId);
        addApplicantToChatRoom(applicantId, groupId);
    }

    @Transactional
    public void rejectJoin(Long groupAdminId, Long applicantId, Long groupId) {
        validator.validateGroupAdminAuthorization(groupAdminId, groupId);

        GroupUser groupUser = getPendingGroupUser(groupId, applicantId);
        groupUser.validateNotAlreadyMember();

        groupUserRepository.delete(groupUser);
        sendJoinRejectionAlarm(applicantId, groupId);
    }

    @Transactional
    public CreateNoticeRes createNotice(CreateNoticeReq request, Long groupAdminId, Long groupId) {
        validator.validateGroupAdminAuthorization(groupAdminId, groupId);

        Group group = groupRepository.getReferenceById(groupId);
        User noticer = userRepository.getReferenceById(groupAdminId);
        Post notice = request.toEntity(noticer, group);
        notice.addImages(request.images());

        postRepository.save(notice);
        sendNoticeAlarms(groupId, groupAdminId, request.title(), notice.getId());

        return new CreateNoticeRes(notice.getId());
    }

    @Transactional
    public void deleteGroup(Long groupId, Long userId) {
        validator.validateGroupExists(groupId);
        validator.validateGroupCreatorAuthorization(groupId, userId);

        deleteGroupRelatedData(groupId);
        deleteGroupPostsAndComments(groupId);
        deleteGroupChatRoom(groupId);

        groupRepository.deleteById(groupId);
    }

    @Transactional
    public void updateUserRole(UpdateUserRoleReq request, Long groupId, Long creatorId) {
        validator.validateGroupExists(groupId);
        validator.validateGroupMembership(groupId, request.userId());

        validator.validateGroupCreatorAuthorization(groupId, creatorId);
        validator.validateRoleUpdateConstraints(request.role(), request.userId(), creatorId);

        groupUserRepository.updateRole(request.role(), groupId, request.userId());
    }

    private Province determineProvince(Long userId) {
        if (userId == null) {
            return DEFAULT_PROVINCE;
        }

        return userRepository.findNonWithdrawnById(userId)
                .map(User::getProvince)
                .orElse(DEFAULT_PROVINCE);
    }

    private List<Long> getLikedGroupIds(Long userId) {
        if (userId == null) {
            return Collections.emptyList();
        }

        return favoriteGroupRepository.findGroupIdByUserId(userId);
    }

    private List<RecommendGroupDTO> fetchGroupsByProvince(Province province, Long userId, List<Long> likedGroupIds) {
        List<Group> groups = groupRepository.findByProvinceWithoutMyGroup(province, userId, GroupRole.TEMP, RECOMMEND_GROUP_PAGEABLE).getContent();
        List<Long> groupIds = groups.stream()
                .map(Group::getId)
                .toList();

        Map<Long, Long> likeCountMap = likeService.getLikeCounts(groupIds, Like.GROUP);
        return RecommendGroupDTO.fromEntities(groups, likeCountMap, likedGroupIds);
    }

    private List<RecommendGroupDTO> fetchAdditionalGroupsIfNeeded(Long userId, List<Long> likedGroupIds, List<RecommendGroupDTO> existingGroups) {
        if (existingGroups.size() >= DEFAULT_RECOMMENDATION_LIMIT) {
            return Collections.emptyList();
        }

        List<Group> groups = groupRepository.findAllWithoutMyGroup(userId, RECOMMEND_GROUP_PAGEABLE).getContent();
        List<Long> groupIds = groups.stream()
                .map(Group::getId)
                .toList();

        Map<Long, Long> likeCountMap = likeService.getLikeCounts(groupIds, Like.GROUP);
        Set<Long> existingGroupIds = existingGroups.stream()
                .map(RecommendGroupDTO::id)
                .collect(Collectors.toSet());

        return RecommendGroupDTO.fromEntities(groups, likeCountMap, likedGroupIds).stream()
                .filter(newGroup -> !existingGroupIds.contains(newGroup.id()))
                .toList();
    }

    private List<RecommendGroupDTO> mergeAndRandomizeGroups(List<RecommendGroupDTO> recommendedGroups, List<RecommendGroupDTO> additionalGroups) {
        List<RecommendGroupDTO> mergedGroups = new ArrayList<>(recommendedGroups);
        mergedGroups.addAll(additionalGroups);
        Collections.shuffle(mergedGroups); // 랜덤화

        return mergedGroups;
    }

    private Province resolveProvince(Long userId, Province inputProvince) {
        if (inputProvince != null) {
            return inputProvince;
        }

        if (userId != null) {
            return userRepository.findProvinceById(userId).orElse(DEFAULT_PROVINCE);
        }

        return DEFAULT_PROVINCE;
    }

    public void checkGroupAndIsMember(Long groupId, Long userId) {
        validator.validateGroupExists(groupId);
        validator.validateGroupMembership(groupId, userId);
    }

    public List<Long> getLikedGroupList(Long userId) {
        return userId != null ? favoriteGroupRepository.findGroupIdByUserId(userId) : Collections.emptyList();
    }

    private User addGroupOwner(Group group, Long ownerId) {
        User groupOwner = userRepository.getReferenceById(ownerId);

        GroupUser groupUser = GroupUser.builder()
                .group(group)
                .user(groupOwner)
                .groupRole(GroupRole.CREATOR)
                .build();
        groupUserRepository.save(groupUser);
        group.incrementParticipantNum();

        return groupOwner;
    }

    private ChatRoom addChatRoom(Group group) {
        ChatRoom chatRoom = ChatRoom.builder()
                .group(group)
                .name(group.getName())
                .build();
        chatRoomRepository.save(chatRoom);

        return chatRoom;
    }

    private void addChatUserToRoom(ChatRoom chatRoom, User groupOwner) {
        ChatUser chatUser = ChatUser.builder()
                .chatRoom(chatRoom)
                .user(groupOwner)
                .build();
        chatUserRepository.save(chatUser);
    }

    private void updateGroupChatRoomName(Long groupId, String groupName) {
        ChatRoom chatRoom = chatRoomRepository.findByGroupId(groupId).orElseThrow(
                () -> new CustomException(ExceptionCode.CHAT_ROOM_NOT_FOUND)
        );
        chatRoom.updateName(groupName);
    }

    private void updateGroupInfo(UpdateGroupReq request, Group group) {
        group.updateInfo(request.name(), request.province(), request.district(), group.getSubDistrict(),
                request.description(), request.category(), request.profileURL(), request.maxNum());
    }

    private List<FindGroupMemberListRes.MemberDetailDTO> getMemberDetails(Long groupId) {
        return groupUserRepository.findByGroupIdWithGroup(groupId).stream()
                .filter(GroupUser::isActiveMember)
                .map(FindGroupMemberListRes.MemberDetailDTO::new)
                .toList();
    }

    public List<NoticeDTO> getNotices(Long userId, Long groupId, Pageable pageable) {
        Set<String> readPostIds = groupCacheService.getReadPostIds(userId);

        return postRepository.findNoticeByGroupIdWithUser(groupId, pageable).getContent().stream()
                .map(notice -> new NoticeDTO(notice, readPostIds.contains(notice.getId().toString())))
                .toList();
    }

    private List<MemberDTO> getMembers(Long groupId) {
        return groupUserRepository.findByGroupIdWithUserInAsc(groupId).stream()
                .filter(GroupUser::isActiveMember)
                .map(MemberDTO::new)
                .toList();
    }

    // 1. 같은 지역의 그룹  2. 좋아요, 사용자 순
    public List<RecommendGroupDTO> getRecommendGroups(Long userId, Province province, List<Long> likedGroupIds) {
        List<RecommendGroupDTO> recommendedGroups = fetchGroupsByProvince(province, userId, likedGroupIds);
        List<RecommendGroupDTO> additionalGroups = fetchAdditionalGroupsIfNeeded(userId, likedGroupIds, recommendedGroups);

        return mergeAndRandomizeGroups(recommendedGroups, additionalGroups).stream()
                .limit(DEFAULT_RECOMMENDATION_LIMIT)
                .toList();
    }

    public List<NewGroupDTO> getNewGroups(Long userId, Province inputProvince, Pageable pageable) {
        Province province = resolveProvince(userId, inputProvince);

        return groupRepository.findByProvinceWithoutMyGroup(province, userId, GroupRole.TEMP, pageable).getContent().stream()
                .map(NewGroupDTO::new)
                .toList();
    }

    public List<MyGroupDTO> getMyGroups(Long userId, List<Long> likedGroupIds, Pageable pageable) {
        if (userId == null) {
            return Collections.emptyList();
        }

        List<Group> groups = groupUserRepository.findGroupByUserId(userId, pageable).getContent();
        List<Long> groupIds = groups.stream()
                .map(Group::getId)
                .toList();

        Map<Long, Long> likeCountMap = likeService.getLikeCounts(groupIds, Like.GROUP);
        return MyGroupDTO.fromEntities(groups, likeCountMap, likedGroupIds);
    }

    private void addTemporaryGroupMember(User applicant, Group group, String greeting) {
        GroupUser groupUser = GroupUser.builder()
                .groupRole(GroupRole.TEMP)
                .user(applicant)
                .group(group)
                .greeting(greeting)
                .build();

        groupUserRepository.save(groupUser);
    }

    private void configureRabbitMQForChatRoom(ChatRoom chatRoom) {
        String queueName = ROOM_QUEUE_PREFIX + chatRoom.getId();
        rabbitMqService.createAndBindQueueToExchange(CHAT_EXCHANGE, queueName);

        String listenerId = ROOM_QUEUE_PREFIX + chatRoom.getId();
        rabbitMqService.registerMessageQueueListener(listenerId, queueName);
    }

    private GroupUser getGroupUser(Long groupId, Long userId) {
        return groupUserRepository.findByGroupIdAndUserId(groupId, userId).orElseThrow(
                () -> new CustomException(ExceptionCode.GROUP_NOT_APPLIED)
        );
    }

    private void approveMembership(GroupUser groupUser, Group group) {
        groupUser.updateRole(GroupRole.USER);
        group.incrementParticipantNum();
    }

    private void notifyApplicant(Long applicantId, Long groupId) {
        String content = "가입이 승인 되었습니다!";
        String redirectURL = "/volunteer/" + groupId;

        alarmService.sendAlarm(applicantId, content, redirectURL, AlarmType.JOIN);
    }

    private void addApplicantToChatRoom(Long applicantId, Long groupId) {
        ChatRoom chatRoom = chatRoomRepository.findByGroupId(groupId).orElseThrow(
                () -> new CustomException(ExceptionCode.CHAT_ROOM_NOT_FOUND)
        );

        User applicant = userRepository.getReferenceById(applicantId);
        ChatUser chatUser = ChatUser.builder()
                .user(applicant)
                .chatRoom(chatRoom)
                .build();

        chatUserRepository.save(chatUser);
    }

    private GroupUser getPendingGroupUser(Long groupId, Long applicantId) {
        return groupUserRepository.findByGroupIdAndUserId(groupId, applicantId)
                .orElseThrow(() -> new CustomException(ExceptionCode.GROUP_NOT_APPLIED));
    }

    private void sendJoinRejectionAlarm(Long applicantId, Long groupId) {
        String content = "가입이 거절 되었습니다.";
        String redirectURL = "/volunteer/" + groupId;
        alarmService.sendAlarm(applicantId, content, redirectURL, AlarmType.JOIN);
    }

    private void sendNoticeAlarms(Long groupId, Long senderId, String title, Long noticeId) {
        List<User> groupMembers = groupUserRepository.findUserByGroupIdWithoutMe(groupId, senderId);

        String content = "공지: " + title;
        String redirectURL = "/volunteer/" + groupId + "/notices/" + noticeId;

        groupMembers.forEach(member -> alarmService.sendAlarm(member.getId(), content, redirectURL, AlarmType.NOTICE));
    }

    private void deleteGroupRelatedData(Long groupId) {
        meetingUserRepository.deleteByGroupId(groupId);
        meetingRepository.deleteByGroupId(groupId);
        favoriteGroupRepository.deleteByGroupId(groupId);
        groupUserRepository.deleteByGroupId(groupId);
    }

    private void deleteGroupPostsAndComments(Long groupId) {
        postLikeRepository.deleteByGroupId(groupId);
        commentLikeRepository.deleteByGroupId(groupId);
        commentRepository.hardDeleteChildByGroupId(groupId);
        commentRepository.hardDeleteParentByGroupId(groupId);
        postImageRepository.deleteByGroupId(groupId);
        postRepository.hardDeleteByGroupId(groupId);
        likeService.clearLikeCounts(groupId, Like.GROUP);
    }

    private void deleteGroupChatRoom(Long groupId) {
        ChatRoom chatRoom = chatRoomRepository.findByGroupId(groupId)
                .orElseThrow(() -> new CustomException(ExceptionCode.CHAT_ROOM_NOT_FOUND));
        String queueName = ROOM_QUEUE_PREFIX + chatRoom.getId();
        chatUserRepository.deleteByGroupId(groupId);
        chatRoomRepository.delete(chatRoom);
        rabbitMqService.deleteQueue(queueName); // 채팅방 큐 삭제
    }
}