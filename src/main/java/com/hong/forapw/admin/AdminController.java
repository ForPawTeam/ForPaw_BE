package com.hong.forapw.admin;

import com.hong.forapw.admin.model.request.*;
import com.hong.forapw.admin.model.response.*;
import com.hong.forapw.security.userdetails.CustomUserDetails;
import com.hong.forapw.common.utils.ApiUtils;
import com.hong.forapw.domain.apply.constant.ApplyStatus;
import com.hong.forapw.domain.inquiry.constant.InquiryStatus;
import com.hong.forapw.admin.constant.ReportStatus;

import com.hong.forapw.domain.user.service.UserService;
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

import static com.hong.forapw.common.constants.GlobalConstants.SORT_BY_ID;

@Controller
@RequiredArgsConstructor
@RequestMapping("/api")
public class AdminController {

    private final AdminService authenticationService;
    private final UserService userService;

    @GetMapping("/admin/dashboard")
    public ResponseEntity<?> findDashboardStats() {
        FindDashboardStatsRes response = authenticationService.findDashboardStats();
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, response));
    }

    @GetMapping("/admin/user")
    public ResponseEntity<?> findUsers(@PageableDefault(sort = SORT_BY_ID, direction = Sort.Direction.DESC) Pageable pageable, @AuthenticationPrincipal CustomUserDetails userDetails) {
        FindUserListRes response = authenticationService.findUsers(pageable);
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, response));
    }

    @PatchMapping("/admin/user/role")
    public ResponseEntity<?> changeUserRole(@RequestBody @Valid ChangeUserRoleReq request, @AuthenticationPrincipal CustomUserDetails userDetails) {
        authenticationService.changeUserRole(request, userDetails.user().getRole());
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, null));
    }

    @PostMapping("/admin/user/suspend")
    public ResponseEntity<?> suspendUser(@RequestBody @Valid SuspendUserReq request, @AuthenticationPrincipal CustomUserDetails userDetails) {
        authenticationService.suspendUser(request, userDetails.user().getRole());
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, null));
    }

    @PostMapping("/admin/user/unsuspend")
    public ResponseEntity<?> unSuspendUser(@RequestBody @Valid UnSuspendUserReq request, @AuthenticationPrincipal CustomUserDetails userDetails) {
        authenticationService.unSuspendUser(request.userId(), userDetails.user().getRole());
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, null));
    }

    @DeleteMapping("/admin/user")
    public ResponseEntity<?> withdrawUser(@RequestBody @Valid WithdrawUserReq request) {
        userService.withdrawMember(request.userId());
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, null));
    }

    @GetMapping("/admin/adoption")
    public ResponseEntity<?> findApplies(@RequestParam(required = false) ApplyStatus status,
                                         @PageableDefault(size = 5, sort = SORT_BY_ID, direction = Sort.Direction.DESC) Pageable pageable, @AuthenticationPrincipal CustomUserDetails userDetails) {
        FindApplyListRes response = authenticationService.findApplies(status, pageable);
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, response));
    }

    @PatchMapping("/admin/adoption")
    public ResponseEntity<?> changeApplyStatus(@RequestBody @Valid ChangeApplyStatusReq request, @AuthenticationPrincipal CustomUserDetails userDetails) {
        authenticationService.changeApplyStatus(request);
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, null));
    }

    @GetMapping("/admin/reports")
    public ResponseEntity<?> findReports(@RequestParam(required = false) ReportStatus status,
                                         @PageableDefault(size = 5, sort = SORT_BY_ID, direction = Sort.Direction.DESC) Pageable pageable, @AuthenticationPrincipal CustomUserDetails userDetails) {
        FindReportListRes response = authenticationService.findReports(status, pageable);
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, response));
    }

    @PatchMapping("/admin/reports")
    public ResponseEntity<?> processReport(@RequestBody @Valid ProcessReportReq request, @AuthenticationPrincipal CustomUserDetails userDetails) {
        authenticationService.processReport(request);
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, null));
    }

    @GetMapping("/admin/supports")
    public ResponseEntity<?> findSupportList(@RequestParam(required = false) InquiryStatus status,
                                             @PageableDefault(size = 5, sort = SORT_BY_ID, direction = Sort.Direction.DESC) Pageable pageable, @AuthenticationPrincipal CustomUserDetails userDetails) {
        FindSupportListRes response = authenticationService.findSupportList(status, pageable);
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, response));
    }

    @GetMapping("/admin/supports/{inquiryId}")
    public ResponseEntity<?> findSupportById(@PathVariable Long inquiryId, @AuthenticationPrincipal CustomUserDetails userDetails) {
        FindSupportByIdRes response = authenticationService.findSupportById(inquiryId);
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, response));
    }

    @PostMapping("/admin/supports/{inquiryId}/answer")
    public ResponseEntity<?> answerInquiry(@RequestBody @Valid AnswerInquiryReq request, @PathVariable Long inquiryId, @AuthenticationPrincipal CustomUserDetails userDetails) {
        AnswerInquiryRes response = authenticationService.answerInquiry(request, userDetails.user().getId(), inquiryId);
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, response));
    }

    @GetMapping("/faq")
    public ResponseEntity<?> findFAQList() {
        FindFAQListRes response = authenticationService.findFAQList();
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, response));
    }

    @PostMapping("/faq")
    public ResponseEntity<?> createFAQ(@RequestBody @Valid CreateFaqReq request, @AuthenticationPrincipal CustomUserDetails userDetails) {
        authenticationService.createFAQ(request);
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, null));
    }
}
