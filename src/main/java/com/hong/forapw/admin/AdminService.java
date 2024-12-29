package com.hong.forapw.admin;

import com.hong.forapw.admin.model.request.*;
import com.hong.forapw.admin.model.response.*;
import com.hong.forapw.admin.repository.ReportRepository;
import com.hong.forapw.domain.apply.ApplyRepository;
import com.hong.forapw.common.exceptions.CustomException;
import com.hong.forapw.common.exceptions.ExceptionCode;
import com.hong.forapw.domain.apply.entity.Apply;
import com.hong.forapw.domain.apply.constant.ApplyStatus;
import com.hong.forapw.admin.entity.Visit;
import com.hong.forapw.domain.faq.FAQ;
import com.hong.forapw.domain.faq.FaqRepository;
import com.hong.forapw.domain.inquiry.entity.Inquiry;
import com.hong.forapw.domain.inquiry.constant.InquiryStatus;
import com.hong.forapw.domain.post.entity.Comment;
import com.hong.forapw.domain.post.entity.Post;
import com.hong.forapw.admin.entity.Report;
import com.hong.forapw.admin.constant.ReportStatus;
import com.hong.forapw.domain.user.entity.User;
import com.hong.forapw.domain.user.constant.UserRole;
import com.hong.forapw.domain.user.entity.UserStatus;
import com.hong.forapw.domain.animal.repository.AnimalRepository;
import com.hong.forapw.admin.repository.VisitRepository;
import com.hong.forapw.domain.inquiry.InquiryRepository;
import com.hong.forapw.domain.post.repository.CommentRepository;
import com.hong.forapw.domain.post.repository.PostRepository;
import com.hong.forapw.domain.user.repository.UserRepository;
import com.hong.forapw.domain.user.repository.UserStatusRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminService {

    private final VisitRepository visitRepository;
    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final AnimalRepository animalRepository;
    private final ReportRepository reportRepository;
    private final ApplyRepository applyRepository;
    private final InquiryRepository inquiryRepository;
    private final UserStatusRepository userStatusRepository;
    private final FaqRepository faqRepository;
    private final AdminValidator validator;

    private static final String POST_SCREENED = "이 게시글은 커뮤니티 규정을 위반하여 숨겨졌습니다.";
    private static final String COMMENT_SCREENED = "커뮤니티 규정을 위반하여 가려진 댓글입니다.";


    public FindDashboardStatsRes findDashboardStats() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfDay = truncateToMidnight(now);

        FindDashboardStatsRes.UserStatsDTO userStatsDTO = getUserStats();
        FindDashboardStatsRes.AnimalStatsDTO animalStatsDTO = getAnimalStats(now);

        List<FindDashboardStatsRes.DailyVisitorDTO> dailyVisitorDTOS = getDailyVisitors(now.minusWeeks(1));
        List<FindDashboardStatsRes.HourlyVisitorDTO> hourlyVisitorDTOS = getHourlyVisitors(startOfDay);

        FindDashboardStatsRes.DailySummaryDTO dailySummaryDTO = getDailySummary(startOfDay);

        return new FindDashboardStatsRes(userStatsDTO, animalStatsDTO, dailyVisitorDTOS, hourlyVisitorDTOS, dailySummaryDTO);
    }

    public FindUserListRes findUsers(Pageable pageable) {
        Map<Long, Visit> latestVisitMap = getLatestVisits();

        Map<Long, Long> processingApplyMap = getApplyCountByStatus(ApplyStatus.PROCESSING);
        Map<Long, Long> processedApplyMap = getApplyCountByStatus(ApplyStatus.PROCESSED);

        Page<User> userPage = userRepository.findAll(pageable);
        List<FindUserListRes.ApplicantDTO> applicantDTOs = FindUserListRes.ApplicantDTO.fromEntities(userPage.getContent(), latestVisitMap, processingApplyMap, processedApplyMap);

        return new FindUserListRes(applicantDTOs, userPage.getTotalPages());
    }

    @Transactional
    public void changeUserRole(ChangeUserRoleReq request, UserRole adminRole) {
        User user = userRepository.findNonWithdrawnById(request.userId()).orElseThrow(
                () -> new CustomException(ExceptionCode.USER_NOT_FOUND)
        );

        validator.validateRoleIsDifferent(request.role(), user.getRole());
        validator.prohibitSuperRoleAssignment(request.role());
        validator.validateAdminCannotModifySuper(adminRole, user.getRole());

        user.updateRole(request.role());
    }

    @Transactional
    public void suspendUser(SuspendUserReq request, UserRole adminRole) {
        UserStatus userStatus = userStatusRepository.findByUserId(request.userId()).orElseThrow(
                () -> new CustomException(ExceptionCode.USER_NOT_FOUND)
        );

        validator.validateNotAlreadySuspended(userStatus);
        validator.validateAdminCannotModifySuper(adminRole, userStatus.getUser().getRole());

        userStatus.suspendUser(LocalDateTime.now(), request.suspensionDays(), request.suspensionReason());
    }

    @Transactional
    public void unSuspendUser(Long userId, UserRole adminRole) {
        UserStatus userStatus = userStatusRepository.findByUserId(userId).orElseThrow(
                () -> new CustomException(ExceptionCode.USER_NOT_FOUND)
        );

        validator.validateNotAlreadyUnsuspended(userStatus);
        validator.validateAdminCannotModifySuper(adminRole, userStatus.getUser().getRole());

        userStatus.unsuspendUser();
    }

    public FindApplyListRes findApplies(ApplyStatus status, Pageable pageable) {
        Page<Apply> applyPage = applyRepository.findByStatusWithAnimalAndShelter(status, pageable);
        List<FindApplyListRes.ApplyDTO> applyDTOS = FindApplyListRes.ApplyDTO.fromEntities(applyPage.getContent());

        return new FindApplyListRes(applyDTOS, applyPage.getTotalPages());
    }

    @Transactional
    public void changeApplyStatus(ChangeApplyStatusReq request) {
        Apply apply = applyRepository.findByIdWithAnimal(request.id())
                .orElseThrow(() -> new CustomException(ExceptionCode.APPLICATION_NOT_FOUND));

        validator.validateNotAlreadyProcessed(apply);

        updateApplyStatusAndHandleAdoption(apply, request.status());
    }

    public FindReportListRes findReports(ReportStatus status, Pageable pageable) {
        Page<Report> reportPage = reportRepository.findAllByStatus(status, pageable);
        List<FindReportListRes.ReportDTO> reportDTOs = FindReportListRes.ReportDTO.fromEntities(reportPage.getContent());

        return new FindReportListRes(reportDTOs, reportPage.getTotalPages());
    }

    @Transactional
    public void processReport(ProcessReportReq request) {
        Report report = reportRepository.findById(request.id())
                .orElseThrow(() -> new CustomException(ExceptionCode.REPORT_NOT_FOUND));

        validator.validateReportNotProcessed(report);

        if (request.hasSuspension())
            suspendOffender(report, request.suspensionDays());

        if (request.hasBlocking())
            processContentBlocking(report);

        report.updateStatus(ReportStatus.PROCESSED);
    }

    public FindSupportListRes findSupports(InquiryStatus status, Pageable pageable) {
        Page<Inquiry> inquiryPage = inquiryRepository.findByStatusWithUser(status, pageable);
        List<FindSupportListRes.InquiryDTO> inquiryDTOs = FindSupportListRes.InquiryDTO.fromEntities(inquiryPage.getContent());

        return new FindSupportListRes(inquiryDTOs, inquiryPage.getTotalPages());
    }

    public FindSupportByIdRes findSupportById(Long inquiryId) {
        Inquiry inquiry = inquiryRepository.findById(inquiryId)
                .orElseThrow(() -> new CustomException(ExceptionCode.INQUIRY_NOT_FOUND));

        return new FindSupportByIdRes(inquiry);
    }

    @Transactional
    public AnswerInquiryRes answerInquiry(AnswerInquiryReq request, Long adminId, Long inquiryId) {
        Inquiry inquiry = inquiryRepository.findById(inquiryId)
                .orElseThrow(() -> new CustomException(ExceptionCode.INQUIRY_NOT_FOUND));

        validator.validateInquiryNotAnswered(inquiry);

        User admin = userRepository.getReferenceById(adminId);
        inquiry.updateInquiryWithAnswer(request.content(), admin, InquiryStatus.PROCESSED);

        return new AnswerInquiryRes(inquiryId);
    }

    public FindFAQListRes findFAQList() {
        List<FAQ> faqs = faqRepository.findAll();
        List<FindFAQListRes.FaqDTO> faqDTOs = FindFAQListRes.FaqDTO.fromEntities(faqs);

        return new FindFAQListRes(faqDTOs);
    }

    @Transactional
    public void createFAQ(CreateFaqReq request) {
        FAQ faq = request.toEntity();
        faqRepository.save(faq);
    }

    private LocalDateTime truncateToMidnight(LocalDateTime dateTime) {
        return dateTime.withHour(0).withMinute(0).withSecond(0).withNano(0);
    }

    private FindDashboardStatsRes.UserStatsDTO getUserStats() {
        Long activeUserCount = userRepository.countActiveUsers();
        Long notActiveUserCount = userRepository.countNotActiveUsers();

        return new FindDashboardStatsRes.UserStatsDTO(activeUserCount, notActiveUserCount);
    }

    private FindDashboardStatsRes.AnimalStatsDTO getAnimalStats(LocalDateTime now) {
        Long waitingForAdoptionCount = animalRepository.countAnimal();
        Long adoptionProcessingCount = applyRepository.countByStatus(ApplyStatus.PROCESSING);
        Long adoptedRecentlyCount = applyRepository.countByStatusWithinDate(ApplyStatus.PROCESSED, now.minusWeeks(1));
        Long adoptedTotalCount = applyRepository.countByStatus(ApplyStatus.PROCESSED);

        return new FindDashboardStatsRes.AnimalStatsDTO(waitingForAdoptionCount, adoptionProcessingCount, adoptedRecentlyCount, adoptedTotalCount);
    }

    private List<FindDashboardStatsRes.DailyVisitorDTO> getDailyVisitors(LocalDateTime startDate) {
        List<Visit> visits = visitRepository.findALlWithinDate(startDate);
        Map<LocalDate, Long> dailyVisitors = visits.stream()
                .collect(Collectors.groupingBy(Visit::getTruncatedDate, Collectors.counting()));

        return dailyVisitors.entrySet().stream()
                .map(FindDashboardStatsRes.DailyVisitorDTO::new)
                .sorted(Comparator.comparing(FindDashboardStatsRes.DailyVisitorDTO::date))
                .toList();
    }

    private List<FindDashboardStatsRes.HourlyVisitorDTO> getHourlyVisitors(LocalDateTime startOfDay) {
        List<Visit> visits = visitRepository.findALlWithinDate(startOfDay);
        Map<LocalTime, Long> hourlyVisitors = visits.stream()
                .filter(visit -> visit.isSameDate(LocalDate.now()))
                .collect(Collectors.groupingBy(Visit::getTruncatedHour, Collectors.counting()));

        return hourlyVisitors.entrySet().stream()
                .map(FindDashboardStatsRes.HourlyVisitorDTO::new)
                .sorted(Comparator.comparing(FindDashboardStatsRes.HourlyVisitorDTO::hour))
                .toList();
    }

    private FindDashboardStatsRes.DailySummaryDTO getDailySummary(LocalDateTime startOfDay) {
        Long entryNum = userRepository.countAllUsersCreatedAfter(startOfDay);
        Long newPostNum = postRepository.countALlWithinDate(startOfDay);
        Long newCommentNum = commentRepository.countALlWithinDate(startOfDay);
        Long newAdoptApplicationNum = applyRepository.countByStatusWithinDate(ApplyStatus.PROCESSING, startOfDay);

        return new FindDashboardStatsRes.DailySummaryDTO(entryNum, newPostNum, newCommentNum, newAdoptApplicationNum);
    }

    private Map<Long, Visit> getLatestVisits() {
        List<Visit> visits = visitRepository.findAll();
        return visits.stream()
                .collect(Collectors.toMap(
                        visit -> visit.getUser().getId(),
                        visit -> visit,
                        Visit::getLatestVisit)
                );
    }

    private Map<Long, Long> getApplyCountByStatus(ApplyStatus status) {
        List<Apply> applies = applyRepository.findByStatus(status);
        return applies.stream()
                .collect(Collectors.groupingBy(
                        apply -> apply.getUser().getId(),
                        Collectors.counting()
                ));
    }

    private void updateApplyStatusAndHandleAdoption(Apply apply, ApplyStatus status) {
        apply.updateApplyStatus(status);

        if (status.equals(ApplyStatus.PROCESSED)) {
            apply.finishAdoption();
        }
    }

    private void suspendOffender(Report report, Long suspensionDays) {
        if (report.getOffenderRole() == UserRole.SUPER) {
            throw new CustomException(ExceptionCode.ADMIN_CANNOT_BE_REPORTED);
        }

        report.getOffenderStatus()
                .suspendUser(LocalDateTime.now(), suspensionDays, report.getReason());
    }

    private void processContentBlocking(Report report) {
        switch (report.getContentType()) {
            case POST -> blockPostContent(report.getContentId());
            case COMMENT -> blockCommentContent(report.getContentId());
            default -> throw new CustomException(ExceptionCode.BAD_APPROACH);
        }
    }

    private void blockPostContent(Long contentId) {
        Post post = postRepository.findById(contentId)
                .orElseThrow(() -> new CustomException(ExceptionCode.BAD_APPROACH));

        post.updateTitle(POST_SCREENED);
        post.processBlock();
    }

    private void blockCommentContent(Long contentId) {
        Comment comment = commentRepository.findById(contentId)
                .orElseThrow(() -> new CustomException(ExceptionCode.BAD_APPROACH));

        comment.updateContent(COMMENT_SCREENED);
    }
}