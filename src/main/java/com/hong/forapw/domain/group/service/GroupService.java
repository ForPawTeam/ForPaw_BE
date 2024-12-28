package com.hong.forapw.domain.group.service;

import com.hong.forapw.domain.alarm.AlarmService;
import com.hong.forapw.domain.group.entity.Group;
import com.hong.forapw.domain.group.constant.GroupRole;
import com.hong.forapw.domain.group.entity.GroupUser;
import com.hong.forapw.domain.group.model.request.*;
import com.hong.forapw.domain.group.model.response.*;
import com.hong.forapw.domain.group.repository.FavoriteGroupRepository;
import com.hong.forapw.domain.group.repository.GroupRepository;
import com.hong.forapw.domain.group.repository.GroupUserRepository;
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
import com.hong.forapw.domain.post.entity.PostImage;
import com.hong.forapw.domain.region.constant.District;
import com.hong.forapw.domain.region.constant.Province;
import com.hong.forapw.domain.user.entity.User;
import com.hong.forapw.domain.user.constant.UserRole;
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

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

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

    private static final Province DEFAULT_PROVINCE = Province.DAEGU;
    private static final String SORT_BY_ID = "id";
    private static final String SORT_BY_PARTICIPANT_NUM = "participantNum";
    private static final String ROOM_QUEUE_PREFIX = "room.";
    private static final String CHAT_EXCHANGE = "chat.exchange";
    private static final Pageable DEFAULT_PAGE_REQUEST = PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, SORT_BY_ID));
    private static final int DEFAULT_RECOMMENDATION_LIMIT = 5;
    private static final int ADDITIONAL_GROUP_FETCH_LIMIT = 30;
    private static final Sort DEFAULT_SORT = Sort.by(Sort.Order.desc(SORT_BY_PARTICIPANT_NUM));

    @Transactional
    public CreateGroupRes createGroup(CreateGroupReq request, Long creatorId) {
        validateGroupNameNotDuplicate(request.name());

        Group group = request.toEntity();
        groupRepository.save(group);

        User groupOwner = addGroupOwner(group, creatorId);
        ChatRoom chatRoom = addChatRoom(group);
        addChatUserToRoom(chatRoom, groupOwner);

        likeService.initGroupLikeCount(group.getId());
        configureRabbitMQForChatRoom(chatRoom);

        return new CreateGroupRes(group.getId());
    }

    // 클라이언트단에서 수정할 때 사용하는 API
    public FindGroupByIdRes findGroupById(Long groupId, Long userId) {
        User groupAdmin = userRepository.getReferenceById(userId);
        validateGroupAdminAuthorization(groupAdmin, groupId);

        Group group = groupRepository.findById(groupId).orElseThrow(
                () -> new CustomException(ExceptionCode.GROUP_NOT_FOUND)
        );

        return new FindGroupByIdRes(group);
    }

    @Transactional
    public void updateGroup(UpdateGroupReq request, Long groupId, Long userId) {
        User groupAdmin = userRepository.getReferenceById(userId);
        validateGroupAdminAuthorization(groupAdmin, groupId);

        Group group = groupRepository.findById(groupId).orElseThrow(
                () -> new CustomException(ExceptionCode.GROUP_NOT_FOUND)
        );

        validateGroupNameNotDuplicate(groupId, request.name());
        updateChatRoomName(groupId, request.name());

        group.updateInfo(request.name(), request.province(), request.district(), group.getSubDistrict(),
                request.description(), request.category(), request.profileURL(), request.maxNum());
    }

    public FindGroupMemberListRes findGroupMembers(Long userId, Long groupId) {
        Group group = groupRepository.findById(groupId).orElseThrow(
                () -> new CustomException(ExceptionCode.GROUP_NOT_FOUND)
        );

        User groupAdmin = userRepository.getReferenceById(userId);
        validateGroupAdminAuthorization(groupAdmin, groupId);

        List<FindGroupMemberListRes.MemberDetailDTO> memberDetails = getMemberDetails(groupId);
        return new FindGroupMemberListRes(group.getParticipantNum(), group.getMaxNum(), memberDetails);
    }

    public FindAllGroupListRes findGroups(Long userId) {
        Province province = determineProvince(userId);

        List<Long> likedGroupIdList = findLikedGroupIds(userId);
        List<RecommendGroupDTO> recommendGroupDTOS = findRecommendGroups(userId, province, likedGroupIdList);
        List<NewGroupDTO> newGroupDTOS = findNewGroups(userId, province, DEFAULT_PAGE_REQUEST);
        List<MyGroupDTO> myGroupDTOS = findMyGroups(userId, likedGroupIdList);

        return new FindAllGroupListRes(recommendGroupDTOS, newGroupDTOS, myGroupDTOS);
    }

    // 1. 같은 지역의 그룹  2. 좋아요, 사용자 순
    public List<RecommendGroupDTO> findRecommendGroups(Long userId, Province province, List<Long> likedGroupIds) {
        List<RecommendGroupDTO> recommendedGroups = fetchGroupsByProvince(province, userId, likedGroupIds);
        List<RecommendGroupDTO> additionalGroups = fetchAdditionalGroupsIfNeeded(userId, likedGroupIds, recommendedGroups);

        List<RecommendGroupDTO> finalRecommendations = mergeAndRandomizeGroups(recommendedGroups, additionalGroups);

        return finalRecommendations.stream()
                .limit(DEFAULT_RECOMMENDATION_LIMIT)
                .toList();
    }

    public List<LocalGroupDTO> findLocalGroups(Long userId, Province province, District district, List<Long> likedGroupIds, Pageable pageable) {
        List<Group> localGroups = groupRepository.findByProvinceAndDistrictWithoutMyGroup(province, district, userId, GroupRole.TEMP, pageable).getContent();

        return localGroups.stream()
                .map(group -> new LocalGroupDTO(group, likeService.getGroupLikeCount(group.getId()), likedGroupIds.contains(group.getId())))
                .toList();
    }

    public List<NewGroupDTO> findNewGroups(Long userId, Province inputProvince, Pageable pageable) {
        Province province = resolveProvince(userId, inputProvince);
        List<Group> newGroups = groupRepository.findByProvinceWithoutMyGroup(province, userId, GroupRole.TEMP, pageable).getContent();

        return newGroups.stream()
                .map(NewGroupDTO::new)
                .toList();
    }

    private List<MyGroupDTO> findMyGroups(Long userId, List<Long> likedGroupIds) {
        if (userId == null) {
            return Collections.emptyList();
        }

        return findMyGroups(userId, likedGroupIds, DEFAULT_PAGE_REQUEST);
    }

    public FindLocalAndNewGroupListRes findLocalAndNewGroups(Long userId, Province province, District district, List<Long> likedGroupIds, Pageable pageable) {
        List<LocalGroupDTO> localGroupDTOS = findLocalGroups(userId, province, district, likedGroupIds, pageable);
        List<NewGroupDTO> newGroupDTOS = findNewGroups(userId, province, pageable);

        return new FindLocalAndNewGroupListRes(localGroupDTOS, newGroupDTOS);
    }

    public List<MyGroupDTO> findMyGroups(Long userId, List<Long> likedGroupIds, Pageable pageable) {
        List<Group> joinedGroups = groupUserRepository.findGroupByUserId(userId, pageable).getContent();

        return joinedGroups.stream()
                .map(group -> new MyGroupDTO(group, likeService.getGroupLikeCount(group.getId()), likedGroupIds.contains(group.getId())))
                .toList();
    }

    public FindGroupDetailByIdRes findGroupDetailById(Long userId, Long groupId) {
        Group group = groupRepository.findById(groupId).orElseThrow(
                () -> new CustomException(ExceptionCode.GROUP_NOT_FOUND)
        );

        List<MeetingDTO> meetingDTOs = meetingService.findMeetings(groupId, DEFAULT_PAGE_REQUEST);
        List<FindGroupDetailByIdRes.MeetingDTO> convertedMeetingDTOs = FindGroupDetailByIdRes.fromMeetingResList(meetingDTOs);

        List<NoticeDTO> noticeDTOs = findNotices(userId, groupId, DEFAULT_PAGE_REQUEST);
        List<MemberDTO> memberDTOs = findMembers(groupId);

        return new FindGroupDetailByIdRes(group, noticeDTOs, convertedMeetingDTOs, memberDTOs);
    }

    public List<NoticeDTO> findNotices(Long userId, Long groupId, Pageable pageable) {
        List<Post> notices = postRepository.findNoticeByGroupIdWithUser(groupId, pageable).getContent();
        Set<String> readPostIds = groupCacheService.getReadPostIds(userId);

        return notices.stream()
                .map(notice -> new NoticeDTO(notice, readPostIds.contains(notice.getId().toString())))
                .toList();
    }

    @Transactional
    public void joinGroup(JoinGroupReq request, Long userId, Long groupId) {
        Group group = groupRepository.findById(groupId).orElseThrow(
                () -> new CustomException(ExceptionCode.GROUP_NOT_FOUND)
        );

        validateGroupCapacity(group);
        validateUserNotAlreadyMemberOrApplicant(groupId, userId);

        User applicant = userRepository.getReferenceById(userId);
        addTemporaryGroupMember(applicant, group, request.greeting());
    }

    @Transactional
    public void withdrawGroup(Long userId, Long groupId) {
        Group group = groupRepository.findById(groupId).orElseThrow(
                () -> new CustomException(ExceptionCode.GROUP_NOT_FOUND)
        );

        validateIsGroupMember(groupId, userId);

        groupUserRepository.deleteByGroupIdAndUserId(groupId, userId);
        chatUserRepository.deleteByGroupIdAndUserId(groupId, userId);
        meetingUserRepository.deleteByGroupIdAndUserId(groupId, userId);

        group.decrementParticipantNum();
        meetingRepository.decrementParticipantCountForUserMeetings(groupId, userId);
    }

    @Transactional
    public void expelGroupMember(Long adminId, Long memberId, Long groupId) {
        Group group = groupRepository.findById(groupId).orElseThrow(
                () -> new CustomException(ExceptionCode.GROUP_NOT_FOUND)
        );

        User groupAdmin = userRepository.getReferenceById(adminId);
        validateGroupAdminAuthorization(groupAdmin, groupId);

        groupUserRepository.deleteByGroupIdAndUserId(groupId, memberId);
        chatUserRepository.deleteByGroupIdAndUserId(groupId, memberId);
        meetingUserRepository.deleteByGroupIdAndUserId(groupId, memberId);

        group.decrementParticipantNum();
        meetingRepository.decrementParticipantCountForUserMeetings(groupId, memberId);
    }

    public FindApplicantListRes findApplicants(Long adminId, Long groupId) {
        validateGroupExists(groupId);

        User groupAdmin = userRepository.getReferenceById(adminId);
        validateGroupAdminAuthorization(groupAdmin, groupId);

        List<GroupUser> applicants = groupUserRepository.findByGroupRole(groupId, GroupRole.TEMP);
        List<FindApplicantListRes.ApplicantDTO> applicantDTOS = FindApplicantListRes.ApplicantDTO.toDTOs(applicants);

        return new FindApplicantListRes(applicantDTOS);
    }

    @Transactional
    public void approveJoin(Long adminId, Long applicantId, Long groupId) {
        Group group = groupRepository.findById(groupId).orElseThrow(
                () -> new CustomException(ExceptionCode.GROUP_NOT_FOUND)
        );

        User groupAdmin = userRepository.getReferenceById(adminId);
        validateGroupAdminAuthorization(groupAdmin, groupId);

        GroupUser groupUser = findGroupUser(groupId, applicantId);
        validateNotAlreadyMember(groupUser);

        approveMembership(groupUser, group);
        notifyApplicant(applicantId, groupId);
        addApplicantToChatRoom(applicantId, groupId);
    }

    @Transactional
    public void rejectJoin(Long userId, Long applicantId, Long groupId) {
        validateGroupExists(groupId);

        User groupAdmin = userRepository.getReferenceById(userId);
        validateGroupAdminAuthorization(groupAdmin, groupId);

        GroupUser groupUser = findPendingGroupUser(groupId, applicantId);
        validateNotAlreadyMember(groupUser);

        groupUserRepository.delete(groupUser);

        sendJoinRejectionAlarm(applicantId, groupId);
    }

    @Transactional
    public CreateNoticeRes createNotice(CreateNoticeReq request, Long userId, Long groupId) {
        validateGroupExists(groupId);

        User groupAdmin = userRepository.getReferenceById(userId);
        validateGroupAdminAuthorization(groupAdmin, groupId);

        Group group = groupRepository.getReferenceById(groupId);
        User noticer = userRepository.getReferenceById(userId);
        Post notice = request.toEntity(noticer, group);
        addImagesToNotice(request.images(), notice);

        postRepository.save(notice);
        sendNoticeAlarms(groupId, userId, request.title(), notice.getId());

        return new CreateNoticeRes(notice.getId());
    }

    @Transactional
    public void deleteGroup(Long groupId, Long userId) {
        // 존재하지 않는 그룹이면 에러
        validateGroupExists(groupId);

        // 권한체크
        checkGroupCreatorAuthority(groupId, userId);

        // 그룹, 미팅 연관 데이터 삭제
        meetingUserRepository.deleteByGroupId(groupId);
        meetingRepository.deleteByGroupId(groupId);
        favoriteGroupRepository.deleteByGroupId(groupId);
        groupUserRepository.deleteByGroupId(groupId);

        // 그룹과 관련된 게시글, 댓글 관련 데이터 삭제
        postLikeRepository.deleteByGroupId(groupId);
        commentLikeRepository.deleteByGroupId(groupId);
        commentRepository.hardDeleteChildByGroupId(groupId);
        commentRepository.hardDeleteParentByGroupId(groupId);
        postImageRepository.deleteByGroupId(groupId);
        postRepository.hardDeleteByGroupId(groupId);
        likeService.clearGroupLikeData(groupId);

        // 그룹 채팅방 삭제
        ChatRoom chatRoom = chatRoomRepository.findByGroupId(groupId).orElseThrow(
                () -> new CustomException(ExceptionCode.CHAT_ROOM_NOT_FOUND)
        );
        String queueName = ROOM_QUEUE_PREFIX + chatRoom.getId();
        chatUserRepository.deleteByGroupId(groupId);
        chatRoomRepository.delete(chatRoom);
        rabbitMqService.deleteQueue(queueName); // 채팅방 큐 삭제

        groupRepository.deleteById(groupId);
    }

    @Transactional
    public void updateUserRole(UpdateUserRoleReq requestDTO, Long groupId, Long creatorId) {
        // 존재하지 않는 그룹이면 에러
        validateGroupExists(groupId);

        // 가입되지 않은 회원이면 에러
        if (!groupUserRepository.existsByGroupIdAndUserId(groupId, requestDTO.userId())) {
            throw new CustomException(ExceptionCode.GROUP_NOT_MEMBER);
        }

        // 권한체크 (그룹장만 변경 가능)
        checkGroupCreatorAuthority(groupId, creatorId);

        // 그룹장은 자신의 역할을 변경할 수 없음
        if (requestDTO.userId().equals(creatorId)) {
            throw new CustomException(ExceptionCode.CANT_UPDATE_FOR_CREATOR);
        }

        // 그룹장으로의 변경은 불가능
        if (requestDTO.role().equals(GroupRole.CREATOR)) {
            throw new CustomException(ExceptionCode.ROLE_CANT_UPDATE);
        }

        groupUserRepository.updateRole(requestDTO.role(), groupId, requestDTO.userId());
    }

    private Province determineProvince(Long userId) {
        if (userId == null) {
            return DEFAULT_PROVINCE;
        }

        return userRepository.findNonWithdrawnById(userId)
                .map(User::getProvince)
                .orElse(DEFAULT_PROVINCE);
    }

    private List<Long> findLikedGroupIds(Long userId) {
        if (userId == null) {
            return Collections.emptyList();
        }

        return favoriteGroupRepository.findGroupIdByUserId(userId);
    }

    private List<RecommendGroupDTO> fetchGroupsByProvince(Province province, Long userId, List<Long> likedGroupIds) {
        Pageable pageable = PageRequest.of(0, ADDITIONAL_GROUP_FETCH_LIMIT, DEFAULT_SORT);
        return groupRepository.findByProvinceWithoutMyGroup(province, userId, GroupRole.TEMP, pageable).getContent().stream()
                .map(group -> new RecommendGroupDTO(group, likeService.getGroupLikeCount(group.getId()), likedGroupIds.contains(group.getId())))
                .toList();
    }

    private List<RecommendGroupDTO> fetchAdditionalGroupsIfNeeded(Long userId, List<Long> likedGroupIds, List<RecommendGroupDTO> existingGroups) {
        if (existingGroups.size() >= DEFAULT_RECOMMENDATION_LIMIT) {
            return Collections.emptyList();
        }

        Pageable pageable = PageRequest.of(0, ADDITIONAL_GROUP_FETCH_LIMIT, DEFAULT_SORT);
        return groupRepository.findAllWithoutMyGroup(userId, pageable).stream()
                .map(group -> new RecommendGroupDTO(group, likeService.getGroupLikeCount(group.getId()), likedGroupIds.contains(group.getId())))
                .filter(newGroup -> existingGroups.stream().noneMatch(existingGroup -> existingGroup.id().equals(newGroup.id())))
                .toList();
    }

    private List<RecommendGroupDTO> mergeAndRandomizeGroups(List<RecommendGroupDTO> recommendedGroups, List<RecommendGroupDTO> additionalGroups) {
        List<RecommendGroupDTO> mergedGroups = new ArrayList<>(recommendedGroups);
        mergedGroups.addAll(additionalGroups);
        Collections.shuffle(mergedGroups); // 랜덤화

        return mergedGroups;
    }

    // 1. 로그인된 상태고 province가 요청값으로 들어오지 않으면 프로필에서 설정한 province 사용, 2. 로그인도 되지 않으면 디폴트 값 사용
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
        // 그룹 존재 여부 체크
        validateGroupExists(groupId);

        // 맴버인지 체크
        validateIsGroupMember(groupId, userId);
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

    private void validateGroupNameNotDuplicate(String groupName) {
        if (groupRepository.existsByName(groupName)) {
            throw new CustomException(ExceptionCode.GROUP_NAME_EXIST);
        }
    }

    private void validateGroupNameNotDuplicate(Long groupId, String groupName) {
        if (groupRepository.existsByNameExcludingId(groupName, groupId)) {
            throw new CustomException(ExceptionCode.GROUP_NAME_EXIST);
        }
    }

    private void updateChatRoomName(Long groupId, String groupName) {
        ChatRoom chatRoom = chatRoomRepository.findByGroupId(groupId).orElseThrow(
                () -> new CustomException(ExceptionCode.CHAT_ROOM_NOT_FOUND)
        );
        chatRoom.updateName(groupName);
    }

    private List<FindGroupMemberListRes.MemberDetailDTO> getMemberDetails(Long groupId) {
        return groupUserRepository.findByGroupIdWithGroup(groupId).stream()
                .filter(GroupUser::isActiveMember)
                .map(FindGroupMemberListRes.MemberDetailDTO::new)
                .toList();
    }

    private void validateIsGroupMember(Long groupId, Long userId) {
        Set<GroupRole> groupRoles = EnumSet.of(GroupRole.USER, GroupRole.ADMIN, GroupRole.CREATOR);
        groupUserRepository.findByGroupIdAndUserId(groupId, userId)
                .filter(groupUser -> groupRoles.contains(groupUser.getGroupRole()))
                .orElseThrow(() -> new CustomException(ExceptionCode.GROUP_NOT_MEMBER));
    }

    private void checkGroupCreatorAuthority(Long groupId, Long userId) {
        // 서비스 운영자는 접근 가능
        if (checkServiceAdminAuthority(userId)) {
            return;
        }

        // CREATOR만 허용
        groupUserRepository.findByGroupIdAndUserId(groupId, userId)
                .filter(groupUser -> EnumSet.of(GroupRole.CREATOR).contains(groupUser.getGroupRole()))
                .orElseThrow(() -> new CustomException(ExceptionCode.USER_FORBIDDEN));
    }

    private boolean checkServiceAdminAuthority(Long userId) {
        UserRole role = userRepository.findRoleById(userId).orElseThrow(
                () -> new CustomException(ExceptionCode.USER_NOT_FOUND)
        );

        return role.equals(UserRole.ADMIN) || role.equals(UserRole.SUPER);
    }

    private void validateNotAlreadyMember(GroupUser groupUser) {
        if (groupUser.isMember()) {
            throw new CustomException(ExceptionCode.GROUP_ALREADY_JOIN);
        }
    }

    private void validateGroupExists(Long groupId) {
        if (!groupRepository.existsById(groupId)) {
            throw new CustomException(ExceptionCode.GROUP_NOT_FOUND);
        }
    }

    private List<MemberDTO> findMembers(Long groupId) {
        List<GroupUser> groupUsers = groupUserRepository.findByGroupIdWithUserInAsc(groupId);

        return groupUsers.stream()
                .filter(GroupUser::isActiveMember)
                .map(MemberDTO::new)
                .toList();
    }

    private void validateGroupCapacity(Group group) {
        if (group.getMaxNum().equals(group.getParticipantNum())) {
            throw new CustomException(ExceptionCode.GROUP_FULL);
        }
    }

    private void validateUserNotAlreadyMemberOrApplicant(Long groupId, Long userId) {
        Optional<GroupUser> groupUserOP = groupUserRepository.findByGroupIdAndUserId(groupId, userId);
        if (groupUserOP.isPresent()) {
            throw new CustomException(ExceptionCode.GROUP_ALREADY_JOIN);
        }
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
        // 그룹 채팅방의 exchange 등록 후 그룹장에 대한 큐와 리스너 등록
        String queueName = ROOM_QUEUE_PREFIX + chatRoom.getId();
        String listenerId = ROOM_QUEUE_PREFIX + chatRoom.getId();

        rabbitMqService.bindDirectExchangeToQueue(CHAT_EXCHANGE, queueName);
        rabbitMqService.registerChatListener(listenerId, queueName);
    }

    private GroupUser findGroupUser(Long groupId, Long userId) {
        return groupUserRepository.findByGroupIdAndUserId(groupId, userId).orElseThrow(
                () -> new CustomException(ExceptionCode.GROUP_NOT_APPLY)
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

    private GroupUser findPendingGroupUser(Long groupId, Long applicantId) {
        return groupUserRepository.findByGroupIdAndUserId(groupId, applicantId)
                .orElseThrow(() -> new CustomException(ExceptionCode.GROUP_NOT_APPLY));
    }


    private void sendJoinRejectionAlarm(Long applicantId, Long groupId) {
        String content = "가입이 거절 되었습니다.";
        String redirectURL = "/volunteer/" + groupId;
        alarmService.sendAlarm(applicantId, content, redirectURL, AlarmType.JOIN);
    }

    private void addImagesToNotice(List<CreateNoticeReq.PostImageDTO> images, Post notice) {
        images.stream()
                .map(imageDTO -> PostImage.builder()
                        .imageURL(imageDTO.imageURL())
                        .build())
                .forEach(notice::addImage);
    }

    private void sendNoticeAlarms(Long groupId, Long senderId, String title, Long noticeId) {
        List<User> groupMembers = groupUserRepository.findUserByGroupIdWithoutMe(groupId, senderId);

        String content = "공지: " + title;
        String redirectURL = "/volunteer/" + groupId + "/notices/" + noticeId;

        groupMembers.forEach(member -> alarmService.sendAlarm(member.getId(), content, redirectURL, AlarmType.NOTICE));
    }


    private void validateGroupAdminAuthorization(User user, Long groupId) {
        if (user.isAdmin()) {
            return;
        }

        groupUserRepository.findByGroupIdAndUserId(groupId, user.getId())
                .filter(groupUser -> EnumSet.of(GroupRole.ADMIN, GroupRole.CREATOR).contains(groupUser.getGroupRole()))
                .orElseThrow(() -> new CustomException(ExceptionCode.USER_FORBIDDEN));
    }
}