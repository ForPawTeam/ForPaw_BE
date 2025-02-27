package com.hong.forapw.domain.group;

import com.hong.forapw.domain.group.model.request.*;
import com.hong.forapw.domain.group.model.response.*;
import com.hong.forapw.domain.like.common.Like;
import com.hong.forapw.security.userdetails.CustomUserDetails;
import com.hong.forapw.common.utils.ApiUtils;
import com.hong.forapw.domain.region.constant.District;
import com.hong.forapw.domain.region.constant.Province;
import com.hong.forapw.domain.user.entity.User;
import com.hong.forapw.domain.group.service.GroupService;
import com.hong.forapw.domain.like.LikeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

import static com.hong.forapw.common.constants.GlobalConstants.SORT_BY_ID;
import static com.hong.forapw.security.userdetails.CustomUserDetails.getUserIdOrNull;

@Controller
@RequiredArgsConstructor
@RequestMapping("/api")
public class GroupController {

    private final GroupService groupService;
    private final LikeService likeService;

    @PostMapping("/groups")
    public ResponseEntity<?> createGroup(@RequestBody @Valid CreateGroupReq request, @AuthenticationPrincipal CustomUserDetails userDetails) {
        CreateGroupRes response = groupService.createGroup(request, userDetails.getUserId());
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, response));
    }

    @GetMapping("/groups/{groupId}")
    public ResponseEntity<?> findGroupById(@PathVariable Long groupId, @AuthenticationPrincipal CustomUserDetails userDetails) {
        FindGroupByIdRes response = groupService.findGroupById(groupId, userDetails.getUserId());
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, response));
    }

    @PatchMapping("/groups/{groupId}")
    public ResponseEntity<?> updateGroup(@RequestBody @Valid UpdateGroupReq request, @PathVariable Long groupId, @AuthenticationPrincipal CustomUserDetails userDetails) {
        groupService.updateGroup(request, groupId, userDetails.getUserId());
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, null));
    }

    @GetMapping("/groups")
    public ResponseEntity<?> findGroups(@AuthenticationPrincipal CustomUserDetails userDetails) {
        FindAllGroupListRes response = groupService.findGroups(getUserIdOrNull(userDetails));
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, response));
    }

    @GetMapping("/groups/local")
    public ResponseEntity<?> findLocalGroups(@RequestParam Province province, @RequestParam District district,
                                             @PageableDefault(size = 5, sort = SORT_BY_ID, direction = Sort.Direction.DESC) Pageable pageable, @AuthenticationPrincipal CustomUserDetails userDetails) {
        List<Long> likedGroupList = groupService.getLikedGroupList(getUserIdOrNull(userDetails));
        List<LocalGroupDTO> localGroupDTOS = groupService.getLocalGroups(getUserIdOrNull(userDetails), province, district, likedGroupList, pageable);
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, new FindLocalGroupListRes(localGroupDTOS)));
    }

    @GetMapping("/groups/new")
    public ResponseEntity<?> findNewGroups(@RequestParam(value = "province", required = false) Province province,
                                           @PageableDefault(size = 5, sort = SORT_BY_ID, direction = Sort.Direction.DESC) Pageable pageable, @AuthenticationPrincipal CustomUserDetails userDetails) {
        List<NewGroupDTO> newGroupDTOs = groupService.getNewGroups(getUserIdOrNull(userDetails), province, pageable);
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, new FindNewGroupListRes(newGroupDTOs)));
    }

    @GetMapping("/groups/localAndNew")
    public ResponseEntity<?> findLocalAndNewGroups(@RequestParam Province province, @RequestParam District district,
                                                   @PageableDefault(size = 5, sort = SORT_BY_ID, direction = Sort.Direction.DESC) Pageable pageable, @AuthenticationPrincipal CustomUserDetails userDetails) {
        List<Long> likedGroupList = groupService.getLikedGroupList(getUserIdOrNull(userDetails));
        FindLocalAndNewGroupListRes response = groupService.findLocalAndNewGroups(getUserIdOrNull(userDetails), province, district, likedGroupList, pageable);
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, response));
    }

    @GetMapping("/groups/my")
    public ResponseEntity<?> findMyGroups(@PageableDefault(size = 5, sort = SORT_BY_ID, direction = Sort.Direction.DESC) Pageable pageable, @AuthenticationPrincipal CustomUserDetails userDetails) {
        List<Long> likedGroupList = groupService.getLikedGroupList(getUserIdOrNull(userDetails));
        List<MyGroupDTO> myGroupDTOs = groupService.getMyGroups(getUserIdOrNull(userDetails), likedGroupList, pageable);
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, new FindMyGroupListRes(myGroupDTOs)));
    }

    @GetMapping("/groups/{groupId}/detail")
    public ResponseEntity<?> findGroupDetailById(@PathVariable Long groupId, @AuthenticationPrincipal CustomUserDetails userDetails) {
        FindGroupDetailByIdRes response = groupService.findGroupDetailById(getUserIdOrNull(userDetails), groupId);
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, response));
    }

    @GetMapping("/groups/{groupId}/notices")
    public ResponseEntity<?> findNotices(@PathVariable Long groupId,
                                         @PageableDefault(size = 5, sort = SORT_BY_ID, direction = Sort.Direction.DESC) Pageable pageable, @AuthenticationPrincipal CustomUserDetails userDetails) {
        groupService.checkGroupAndIsMember(groupId, userDetails.getUserId());
        List<NoticeDTO> noticeDTOS = groupService.getNotices(userDetails.getUserId(), groupId, pageable);
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, new FindNoticeListRes(noticeDTOS)));
    }

    @PostMapping("/groups/{groupId}/join")
    public ResponseEntity<?> joinGroup(@RequestBody @Valid JoinGroupReq request, @PathVariable Long groupId, @AuthenticationPrincipal CustomUserDetails userDetails) {
        groupService.joinGroup(request, userDetails.getUserId(), groupId);
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, null));
    }

    @PostMapping("/groups/{groupId}/withdraw")
    public ResponseEntity<?> withdrawGroup(@PathVariable Long groupId, @AuthenticationPrincipal CustomUserDetails userDetails) {
        groupService.withdrawGroup(userDetails.getUserId(), groupId);
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, null));
    }

    @PostMapping("/groups/{groupId}/expel")
    public ResponseEntity<?> expelGroupMember(@RequestBody @Valid ExpelGroupMemberReq request, @PathVariable Long groupId, @AuthenticationPrincipal CustomUserDetails userDetails) {
        groupService.expelGroupMember(userDetails.getUserId(), request.userId(), groupId);
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, null));
    }

    @GetMapping("/groups/{groupId}/join")
    public ResponseEntity<?> findApplicants(@PathVariable Long groupId, @AuthenticationPrincipal CustomUserDetails userDetails) {
        FindApplicantListRes response = groupService.findApplicants(userDetails.getUserId(), groupId);
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, response));
    }

    @PostMapping("/groups/{groupID}/join/approve")
    public ResponseEntity<?> approveJoin(@RequestBody @Valid ApproveJoinReq request, @PathVariable Long groupID, @AuthenticationPrincipal CustomUserDetails userDetails) {
        groupService.approveJoin(userDetails.getUserId(), request.applicantId(), groupID);
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, null));
    }

    @PostMapping("/groups/{groupID}/join/reject")
    public ResponseEntity<?> rejectJoin(@RequestBody @Valid RejectJoinReq request, @PathVariable Long groupID, @AuthenticationPrincipal CustomUserDetails userDetails) {
        groupService.rejectJoin(userDetails.getUserId(), request.applicantId(), groupID);
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, null));
    }

    @PostMapping("/groups/{groupId}/notices")
    public ResponseEntity<?> createNotice(@RequestBody @Valid CreateNoticeReq request, @PathVariable Long groupId, @AuthenticationPrincipal CustomUserDetails userDetails) {
        CreateNoticeRes response = groupService.createNotice(request, userDetails.getUserId(), groupId);
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, response));
    }

    @PostMapping("/groups/{groupId}/like")
    public ResponseEntity<?> likeGroup(@PathVariable Long groupId, @AuthenticationPrincipal CustomUserDetails userDetails) {
        likeService.like(groupId, userDetails.getUserId(), Like.GROUP);
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, null));
    }

    @DeleteMapping("/groups/{groupId}")
    public ResponseEntity<?> deleteGroup(@PathVariable Long groupId, @AuthenticationPrincipal CustomUserDetails userDetails) {
        groupService.deleteGroup(groupId, userDetails.getUserId());
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, null));
    }

    @PatchMapping("/groups/{groupId}/userRole")
    public ResponseEntity<?> updateUserRole(@RequestBody @Valid UpdateUserRoleReq request, @PathVariable Long groupId, @AuthenticationPrincipal CustomUserDetails userDetails) {
        groupService.updateUserRole(request, groupId, userDetails.getUserId());
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, null));
    }

    @GetMapping("/groups/{groupId}/members")
    public ResponseEntity<?> findGroupMembers(@PathVariable Long groupId, @AuthenticationPrincipal CustomUserDetails userDetails) {
        FindGroupMemberListRes response = groupService.findGroupMembers(userDetails.getUserId(), groupId);
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, response));
    }
}