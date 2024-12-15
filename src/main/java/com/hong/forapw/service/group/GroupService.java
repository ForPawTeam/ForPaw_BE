package com.hong.forapw.service.group;

import com.hong.forapw.controller.dto.AlarmRequest;
import com.hong.forapw.controller.dto.GroupRequest;
import com.hong.forapw.controller.dto.GroupResponse;
import com.hong.forapw.core.errors.CustomException;
import com.hong.forapw.core.errors.ExceptionCode;
import com.hong.forapw.core.utils.mapper.GroupMapper;
import com.hong.forapw.domain.alarm.AlarmType;
import com.hong.forapw.domain.chat.ChatRoom;
import com.hong.forapw.domain.chat.ChatUser;
import com.hong.forapw.domain.group.*;
import com.hong.forapw.domain.post.Post;
import com.hong.forapw.domain.post.PostImage;
import com.hong.forapw.domain.District;
import com.hong.forapw.domain.Province;
import com.hong.forapw.domain.user.User;
import com.hong.forapw.domain.user.UserRole;
import com.hong.forapw.repository.chat.ChatRoomRepository;
import com.hong.forapw.repository.chat.ChatUserRepository;
import com.hong.forapw.repository.group.*;
import com.hong.forapw.repository.post.*;
import com.hong.forapw.repository.UserRepository;
import com.hong.forapw.service.BrokerService;
import com.hong.forapw.service.RedisService;
import com.hong.forapw.service.like.LikeService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.stream.Collectors;

import static com.hong.forapw.core.utils.mapper.GroupMapper.*;

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
    private final RedisService redisService;
    private final BrokerService brokerService;
    private final LikeService likeService;
    private final MeetingService meetingService;

    private static final Province DEFAULT_PROVINCE = Province.DAEGU;
    private static final District DEFAULT_DISTRICT = District.SUSEONG;
    private static final String SORT_BY_ID = "id";
    private static final String SORT_BY_PARTICIPANT_NUM = "participantNum";
    private static final String POST_READ_KEY_PREFIX = "user:readPosts:";
    private static final String GROUP_LIKE_NUM_KEY_PREFIX = "group:like:count";
    private static final String ROOM_QUEUE_PREFIX = "room.";
    private static final String CHAT_EXCHANGE = "chat.exchange";
    private static final Pageable DEFAULT_PAGE_REQUEST = PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, SORT_BY_ID));

    @Transactional
    public GroupResponse.CreateGroupDTO createGroup(GroupRequest.CreateGroupDTO requestDTO, Long creatorId) {
        validateGroupNameNotDuplicate(requestDTO.name());

        Group group = buildGroup(requestDTO);
        groupRepository.save(group);

        User groupOwner = addGroupOwner(group, creatorId);
        ChatRoom chatRoom = addChatRoom(group);
        addChatUserToRoom(chatRoom, groupOwner);

        initializeGroupLikeCount(group);
        configureRabbitMQForChatRoom(chatRoom);

        return new GroupResponse.CreateGroupDTO(group.getId());
    }

    public GroupResponse.FindGroupByIdDTO findGroupById(Long groupId, Long userId) {
        validateAdminAuthorization(groupId, userId);

        Group group = groupRepository.findById(groupId).orElseThrow(
                () -> new CustomException(ExceptionCode.GROUP_NOT_FOUND)
        );

        return toFindGroupByIdDTO(group);
    }

    @Transactional
    public void updateGroup(GroupRequest.UpdateGroupDTO requestDTO, Long groupId, Long userId) {
        validateAdminAuthorization(groupId, userId);

        Group group = groupRepository.findById(groupId).orElseThrow(
                () -> new CustomException(ExceptionCode.GROUP_NOT_FOUND)
        );

        validateGroupNameNotDuplicate(groupId, requestDTO.name());
        updateChatRoomName(groupId, requestDTO.name());

        updateGroupInfo(group, requestDTO);
    }

    @Transactional
    public GroupResponse.FindGroupMemberListDTO findGroupMemberList(Long userId, Long groupId) {
        // 그룹 존재 여부 체크
        Group group = groupRepository.findById(groupId).orElseThrow(
                () -> new CustomException(ExceptionCode.GROUP_NOT_FOUND)
        );

        // 관리자만 멤버들을 볼 수 있음
        User groupAdmin = userRepository.getReferenceById(userId);
        validateGroupAdminAuthorization(groupAdmin, groupId);

        List<GroupResponse.MemberDetailDTO> memberDetailDTOS = groupUserRepository.findByGroupIdWithGroup(groupId).stream()
                .filter(groupUser -> !groupUser.getGroupRole().equals(GroupRole.TEMP))
                .map(GroupMapper::toMemberDetailDTO)
                .toList();

        return new GroupResponse.FindGroupMemberListDTO(group.getParticipantNum(), group.getMaxNum(), memberDetailDTOS);
    }

    public GroupResponse.FindAllGroupListDTO findGroupList(Long userId) {
        Province province = DEFAULT_PROVINCE;
        District district = DEFAULT_DISTRICT;

        // 로그인이 되어 있어서 userId를 받는다면 => 가입 시 기재한 주소를 바탕으로 그룹 조회
        if (userId != null) {
            Optional<User> userOptional = userRepository.findNonWithdrawnById(userId);
            if (userOptional.isPresent()) {
                User user = userOptional.get();
                province = user.getProvince();
                district = user.getDistrict();
            }
        }

        // 이 API의 페이지네이션은 고정적으로 0페이지/5개만 보내줄 것이다.
        Pageable pageable = PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, SORT_BY_ID));

        // 좋아요한 그룹 리스트 (로그인 하지 않았으면 빈 리스트)
        List<Long> likedGroupIdList = userId != null ? favoriteGroupRepository.findGroupIdByUserId(userId) : Collections.emptyList();

        // 추천 그룹 찾기
        List<GroupResponse.RecommendGroupDTO> recommendGroupDTOS = findRecommendGroupList(userId, province, likedGroupIdList);

        // 지역 그룹 찾기
        // List<GroupResponse.LocalGroupDTO> localGroupDTOS = findLocalGroupList(userId, province, district, likedGroupIdList, pageable);

        // 새 그룹 찾기
        List<GroupResponse.NewGroupDTO> newGroupDTOS = findNewGroupList(userId, province, pageable);

        // 내 그룹 찾기, 만약 로그인 되어 있지 않다면, 빈 리스트로 처리한다.
        List<GroupResponse.MyGroupDTO> myGroupDTOS = userId != null ? findMyGroupList(userId, likedGroupIdList, pageable) : Collections.emptyList();

        return new GroupResponse.FindAllGroupListDTO(recommendGroupDTOS, newGroupDTOS, myGroupDTOS);
    }

    public List<GroupResponse.LocalGroupDTO> findLocalGroupList(Long userId, Province province, District district, List<Long> likedGroupIds, Pageable pageable) {
        Page<Group> localGroupPage = groupRepository.findByProvinceAndDistrictWithoutMyGroup(province, district, userId, GroupRole.TEMP, pageable);
        return localGroupPage.getContent().stream()
                .map(group -> {
                    Long likeNum = likeService.getGroupLikeCount(group.getId());
                    boolean isLikedGroup = likedGroupIds.contains(group.getId());
                    return toLocalGroupDTO(group, likeNum, isLikedGroup);
                })
                .collect(Collectors.toList());
    }

    public List<GroupResponse.NewGroupDTO> findNewGroupList(Long userId, Province province, Pageable pageable) {
        // 1. 로그인된 상태고 province가 요청값으로 들어오지 않으면 프로필에서 설정한 province 사용, 2. 로그인도 되지 않으면 디폴트 값 사용
        province = Optional.ofNullable(province)
                .or(() -> Optional.ofNullable(userId)
                        .flatMap(userRepository::findProvinceById))
                .orElse(DEFAULT_PROVINCE);

        Page<Group> newGroupPage = groupRepository.findByProvinceWithoutMyGroup(province, userId, GroupRole.TEMP, pageable);

        return newGroupPage.getContent().stream()
                .map(GroupMapper::toNewGroupDTO)
                .collect(Collectors.toList());
    }

    public GroupResponse.FindLocalAndNewGroupListDTO findLocalAndNewGroupList(Long userId, Province province, District district, List<Long> likedGroupIds, Pageable pageable) {
        List<GroupResponse.LocalGroupDTO> localGroupDTOS = findLocalGroupList(userId, province, district, likedGroupIds, pageable);
        List<GroupResponse.NewGroupDTO> newGroupDTOS = findNewGroupList(userId, province, pageable);

        return new GroupResponse.FindLocalAndNewGroupListDTO(localGroupDTOS, newGroupDTOS);
    }

    public List<GroupResponse.MyGroupDTO> findMyGroupList(Long userId, List<Long> likedGroupIds, Pageable pageable) {
        List<Group> joinedGroups = groupUserRepository.findGroupByUserId(userId, pageable).getContent();
        return joinedGroups.stream()
                .map(group -> {
                    Long likeNum = likeService.getGroupLikeCount(group.getId());
                    boolean isLikedGroup = likedGroupIds.contains(group.getId());
                    return toMyGroupDTO(group, likeNum, isLikedGroup);
                })
                .collect(Collectors.toList());
    }

    public GroupResponse.FindGroupDetailByIdDTO findGroupDetailById(Long userId, Long groupId) {
        Group group = groupRepository.findById(groupId).orElseThrow(
                () -> new CustomException(ExceptionCode.GROUP_NOT_FOUND)
        );

        List<GroupResponse.MeetingDTO> meetingDTOS = meetingService.findMeetings(groupId, DEFAULT_PAGE_REQUEST);
        List<GroupResponse.NoticeDTO> noticeDTOS = findNoticeList(userId, groupId, DEFAULT_PAGE_REQUEST);
        List<GroupResponse.MemberDTO> memberDTOS = findMemberList(groupId);

        return new GroupResponse.FindGroupDetailByIdDTO(group.getProfileURL(), group.getName(), group.getDescription(), noticeDTOS, meetingDTOS, memberDTOS);
    }

    public List<GroupResponse.NoticeDTO> findNoticeList(Long userId, Long groupId, Pageable pageable) {
        // 해당 유저가 읽은 post의 id 목록
        String key = POST_READ_KEY_PREFIX + userId;
        Set<String> postIds = userId != null ? redisService.getMembersOfSet(key) : Collections.emptySet();

        Page<Post> noticePage = postRepository.findNoticeByGroupIdWithUser(groupId, pageable);

        return noticePage.getContent().stream()
                .map(notice -> {
                    boolean isRead = postIds.contains(notice.getId().toString());
                    return toNoticeDTO(notice, isRead);
                })
                .toList();
    }

    @Transactional
    public void joinGroup(GroupRequest.JoinGroupDTO requestDTO, Long userId, Long groupId) {
        Group group = groupRepository.findById(groupId).orElseThrow(
                () -> new CustomException(ExceptionCode.GROUP_NOT_FOUND)
        );

        // 그룹 수용 인원 초과
        if (group.getMaxNum().equals(group.getParticipantNum())) {
            throw new CustomException(ExceptionCode.GROUP_FULL);
        }

        // 이미 가입했거나 신청한 회원이면 에러 처리
        checkAlreadyMemberOrApply(groupId, userId);

        User applicant = userRepository.getReferenceById(userId);
        GroupUser groupUser = GroupUser.builder()
                .groupRole(GroupRole.TEMP)
                .user(applicant)
                .group(group)
                .greeting(requestDTO.greeting())
                .build();

        groupUserRepository.save(groupUser);
    }

    @Transactional
    public void withdrawGroup(Long userId, Long groupId) {
        Group group = groupRepository.findById(groupId).orElseThrow(
                () -> new CustomException(ExceptionCode.GROUP_NOT_FOUND)
        );

        // 가입한 회원이 아니면 에러
        validateIsMember(groupId, userId);

        // 그룹 참가자 수 감소
        group.decrementParticipantNum();
        groupUserRepository.deleteByGroupIdAndUserId(groupId, userId);

        // 그룹 채팅방에서 탈퇴
        chatUserRepository.deleteByGroupIdAndUserId(groupId, userId);

        // 맴버가 가입한 정기모임에서도 탈퇴
        meetingUserRepository.deleteByGroupIdAndUserId(groupId, userId);

        // 참가자 수 감소
        meetingRepository.decrementParticipantNum(groupId, userId);
    }

    @Transactional
    public void expelGroupMember(Long adminId, Long memberId, Long groupId) {
        Group group = groupRepository.findById(groupId).orElseThrow(
                () -> new CustomException(ExceptionCode.GROUP_NOT_FOUND)
        );

        // 권한 체크
        validateAdminAuthorization(groupId, adminId);

        // 그룹 참가자 수 감소
        group.decrementParticipantNum();
        groupUserRepository.deleteByGroupIdAndUserId(groupId, memberId);

        // 그룹 채팅방에서 탈퇴
        chatUserRepository.deleteByGroupIdAndUserId(groupId, memberId);

        // 맴버가 가입한 정기모임에서도 탈퇴
        meetingUserRepository.deleteByGroupIdAndUserId(groupId, memberId);

        // 참가자 수 감소
        meetingRepository.decrementParticipantNum(groupId, memberId);
    }

    @Transactional
    public GroupResponse.FindApplicantListDTO findApplicantList(Long managerId, Long groupId) {
        // 존재하지 않는 그룹이면 에러
        checkGroupExist(groupId);

        // 권한 체크
        validateAdminAuthorization(groupId, managerId);

        List<GroupUser> applicants = groupUserRepository.findByGroupRole(groupId, GroupRole.TEMP);
        List<GroupResponse.ApplicantDTO> applicantDTOS = applicants.stream()
                .map(GroupMapper::toApplicantDTO)
                .toList();

        return new GroupResponse.FindApplicantListDTO(applicantDTOS);
    }

    @Transactional
    public void approveJoin(Long managerId, Long applicantId, Long groupId) {
        // 존재하지 않는 그룹이면 에러
        Group group = groupRepository.findById(groupId).orElseThrow(
                () -> new CustomException(ExceptionCode.GROUP_NOT_FOUND)
        );

        // 권한 체크
        validateAdminAuthorization(groupId, managerId);

        // 신청한 적이 없거나 이미 가입했는지 체크
        GroupUser groupUser = groupUserRepository.findByGroupIdAndUserId(groupId, applicantId).orElseThrow(
                () -> new CustomException(ExceptionCode.GROUP_NOT_APPLY)
        );

        checkAlreadyJoin(groupUser);

        // '임시' => '유저'로 역할 변경
        groupUser.updateRole(GroupRole.USER);

        // 그룹 참가자 수 증가
        group.incrementParticipantNum();

        // 알람 생성
        String content = "가입이 승인 되었습니다!";
        String redirectURL = "/volunteer/" + groupId;
        createAlarm(applicantId, content, redirectURL, AlarmType.JOIN);

        // 신청자는 그룹 채팅방에 참여됨
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

    @Transactional
    public void rejectJoin(Long userId, Long applicantId, Long groupId) {
        // 존재하지 않는 그룹이면 에러
        checkGroupExist(groupId);

        // 권한 체크
        validateAdminAuthorization(groupId, userId);

        // 신청한 적이 없거나 이미 가입했는지 체크
        GroupUser groupUser = groupUserRepository.findByGroupIdAndUserId(groupId, applicantId).orElseThrow(
                () -> new CustomException(ExceptionCode.GROUP_NOT_APPLY)
        );

        checkAlreadyJoin(groupUser);

        groupUserRepository.delete(groupUser);

        // 알람 생성
        String content = "가입이 거절 되었습니다.";
        String redirectURL = "/volunteer/" + groupId;
        createAlarm(applicantId, content, redirectURL, AlarmType.JOIN);
    }

    @Transactional
    public GroupResponse.CreateNoticeDTO createNotice(GroupRequest.CreateNoticeDTO requestDTO, Long userId, Long groupId) {
        // 존재하지 않는 그룹이면 에러
        checkGroupExist(groupId);

        // 권한 체크
        validateAdminAuthorization(groupId, userId);

        Group group = groupRepository.getReferenceById(groupId);
        User noticer = userRepository.getReferenceById(userId);
        List<PostImage> postImages = requestDTO.images().stream()
                .map(postImageDTO -> PostImage.builder()
                        .imageURL(postImageDTO.imageURL())
                        .build())
                .toList();

        Post notice = buildNotice(requestDTO, noticer, group);

        // 연관관계 설정
        postImages.forEach(notice::addImage);

        postRepository.save(notice);

        // 알람 생성
        List<User> users = groupUserRepository.findUserByGroupIdWithoutMe(groupId, userId);

        for (User user : users) {
            String content = "공지: " + requestDTO.title();
            String redirectURL = "/volunteer/" + groupId + "/notices/" + notice.getId();
            createAlarm(user.getId(), content, redirectURL, AlarmType.NOTICE);
        }

        return new GroupResponse.CreateNoticeDTO(notice.getId());
    }

    @Transactional
    public void deleteGroup(Long groupId, Long userId) {
        // 존재하지 않는 그룹이면 에러
        checkGroupExist(groupId);

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

        // 레디스에 저장된 좋아요 수 삭제
        redisService.removeValue(GROUP_LIKE_NUM_KEY_PREFIX, groupId.toString());

        // 그룹 채팅방 삭제
        ChatRoom chatRoom = chatRoomRepository.findByGroupId(groupId).orElseThrow(
                () -> new CustomException(ExceptionCode.CHAT_ROOM_NOT_FOUND)
        );
        String queueName = ROOM_QUEUE_PREFIX + chatRoom.getId();
        chatUserRepository.deleteByGroupId(groupId);
        chatRoomRepository.delete(chatRoom);
        brokerService.deleteQueue(queueName); // 채팅방 큐 삭제

        groupRepository.deleteById(groupId);
    }

    @Transactional
    public void updateUserRole(GroupRequest.UpdateUserRoleDTO requestDTO, Long groupId, Long creatorId) {
        // 존재하지 않는 그룹이면 에러
        checkGroupExist(groupId);

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

    public List<GroupResponse.RecommendGroupDTO> findRecommendGroupList(Long userId, Province province, List<Long> likedGroupIds) {
        // 1. 같은 지역의 그룹  2. 좋아요, 사용자 순
        Sort sort = Sort.by(Sort.Order.desc(SORT_BY_PARTICIPANT_NUM));
        Pageable pageable = PageRequest.of(0, 30, sort);

        Page<Group> groupPage = groupRepository.findByProvinceWithoutMyGroup(province, userId, GroupRole.TEMP, pageable);
        List<GroupResponse.RecommendGroupDTO> allRecommendGroupDTOS = groupPage.getContent().stream()
                .map(group -> {
                    Long likeNum = likeService.getGroupLikeCount(group.getId());
                    boolean isLike = likedGroupIds.contains(group.getId());
                    return toRecommendGroupDTO(group, likeNum, isLike);
                })
                .collect(Collectors.toList());

        // District를 바탕으로 한 추천 그룹의 개수가 부족하면, 다른 District의 그룹을 추가
        if (allRecommendGroupDTOS.size() < 5) {
            List<GroupResponse.RecommendGroupDTO> additionalGroupDTOS = fetchAdditionalGroups(userId, likedGroupIds, sort);
            additionalGroupDTOS.stream()
                    .filter(newGroupDTO -> allRecommendGroupDTOS.stream() // 중복된 그룹을 제외하고 추가
                            .noneMatch(oldGroupDTO -> oldGroupDTO.id().equals(newGroupDTO.id())))
                    .forEach(allRecommendGroupDTOS::add);
        }

        // 매번 동일하게 추천을 할 수는 없으니, 간추린 추천 목록 중에서 5개를 랜덤으로 보내준다.
        Collections.shuffle(allRecommendGroupDTOS);

        return allRecommendGroupDTOS.stream()
                .limit(5)
                .collect(Collectors.toList());
    }

    public void checkGroupAndIsMember(Long groupId, Long userId) {
        // 그룹 존재 여부 체크
        checkGroupExist(groupId);

        // 맴버인지 체크
        validateIsMember(groupId, userId);
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

    private void initializeGroupLikeCount(Group group) {
        redisService.storeValue(GROUP_LIKE_NUM_KEY_PREFIX, group.getId().toString(), "0");
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

    private void updateGroupInfo(Group group, GroupRequest.UpdateGroupDTO requestDTO) {
        group.updateInfo(
                requestDTO.name(),
                requestDTO.province(),
                requestDTO.district(),
                group.getSubDistrict(),
                requestDTO.description(),
                requestDTO.category(),
                requestDTO.profileURL(),
                requestDTO.maxNum()
        );
    }

    private void validateIsMember(Long groupId, Long userId) {
        Set<GroupRole> roles = EnumSet.of(GroupRole.USER, GroupRole.ADMIN, GroupRole.CREATOR);
        groupUserRepository.findByGroupIdAndUserId(groupId, userId)
                .filter(groupUser -> roles.contains(groupUser.getGroupRole()))
                .orElseThrow(() -> new CustomException(ExceptionCode.GROUP_NOT_MEMBER));
    }

    private void checkAlreadyMemberOrApply(Long groupId, Long userId) {
        Optional<GroupUser> groupUserOP = groupUserRepository.findByGroupIdAndUserId(groupId, userId);
        if (groupUserOP.isPresent()) {
            throw new CustomException(ExceptionCode.GROUP_ALREADY_JOIN);
        }
    }

    private void validateAdminAuthorization(Long groupId, Long userId) {
        // 서비스 운영자는 접근 가능
        if (checkServiceAdminAuthority(userId)) {
            return;
        }

        // ADMIN, CREATOR만 허용
        groupUserRepository.findByGroupIdAndUserId(groupId, userId)
                .filter(groupUser -> EnumSet.of(GroupRole.ADMIN, GroupRole.CREATOR).contains(groupUser.getGroupRole()))
                .orElseThrow(() -> new CustomException(ExceptionCode.USER_FORBIDDEN));
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

    private void checkAlreadyJoin(GroupUser groupUser) {
        if (groupUser.getGroupRole().equals(GroupRole.USER) || groupUser.getGroupRole().equals(GroupRole.ADMIN) || groupUser.getGroupRole().equals(GroupRole.CREATOR)) {
            throw new CustomException(ExceptionCode.GROUP_ALREADY_JOIN);
        }
    }

    private void checkGroupExist(Long groupId) {
        if (!groupRepository.existsById(groupId)) {
            throw new CustomException(ExceptionCode.GROUP_NOT_FOUND);
        }
    }

    private List<GroupResponse.MemberDTO> findMemberList(Long groupId) {
        // user를 패치조인 해서 조회
        List<GroupUser> groupUsers = groupUserRepository.findByGroupIdWithUserInAsc(groupId);

        return groupUsers.stream()
                .filter(groupUser -> !groupUser.getGroupRole().equals(GroupRole.TEMP)) // 가입 승인 상태가 아니면 제외
                .map(GroupMapper::toMemberDTO)
                .collect(Collectors.toList());
    }

    private void configureRabbitMQForChatRoom(ChatRoom chatRoom) {
        // 그룹 채팅방의 exchange 등록 후 그룹장에 대한 큐와 리스너 등록
        String queueName = ROOM_QUEUE_PREFIX + chatRoom.getId();
        String listenerId = ROOM_QUEUE_PREFIX + chatRoom.getId();

        brokerService.bindDirectExchangeToQueue(CHAT_EXCHANGE, queueName);
        brokerService.registerChatListener(listenerId, queueName);
    }

    private void createAlarm(Long userId, String content, String redirectURL, AlarmType alarmType) {
        AlarmRequest.AlarmDTO alarmDTO = new AlarmRequest.AlarmDTO(
                userId,
                content,
                redirectURL,
                LocalDateTime.now(),
                alarmType);

        brokerService.sendAlarmToUser(userId, alarmDTO);
    }

    private List<GroupResponse.RecommendGroupDTO> fetchAdditionalGroups(Long userId, List<Long> likedGroupIds, Sort sort) {
        Pageable pageable = PageRequest.of(0, 10, sort);

        return groupRepository.findAllWithoutMyGroup(userId, pageable).getContent().stream()
                .map(group -> {
                    Long likeNum = likeService.getGroupLikeCount(group.getId());
                    boolean isLike = likedGroupIds.contains(group.getId());
                    return toRecommendGroupDTO(group, likeNum, isLike);
                })
                .collect(Collectors.toList());
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