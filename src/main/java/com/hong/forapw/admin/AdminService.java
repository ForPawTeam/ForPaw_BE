package com.hong.forapw.admin;

import com.hong.forapw.admin.model.request.*;
import com.hong.forapw.admin.model.response.*;
import com.hong.forapw.admin.repository.ReportRepository;
import com.hong.forapw.domain.apply.ApplyRepository;
import com.hong.forapw.common.exceptions.CustomException;
import com.hong.forapw.common.exceptions.ExceptionCode;
import com.hong.forapw.domain.animal.entity.Animal;
import com.hong.forapw.domain.apply.entity.Apply;
import com.hong.forapw.domain.apply.constant.ApplyStatus;
import com.hong.forapw.admin.entity.Visit;
import com.hong.forapw.domain.faq.FAQ;
import com.hong.forapw.domain.faq.FaqRepository;
import com.hong.forapw.domain.inquiry.entity.Inquiry;
import com.hong.forapw.domain.inquiry.constant.InquiryStatus;
import com.hong.forapw.domain.post.entity.Comment;
import com.hong.forapw.domain.post.entity.Post;
import com.hong.forapw.admin.constant.ContentType;
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

        List<FindUserListRes.ApplicantDTO> applicantDTOS = userPage.getContent().stream()
                .map(user -> mapToApplicantDTO(user, latestVisitMap, processingApplyMap, processedApplyMap))
                .toList();

        return new FindUserListRes(applicantDTOS, userPage.getTotalPages());
    }

    @Transactional
    public void changeUserRole(ChangeUserRoleReq request, UserRole adminRole) {
        User user = userRepository.findNonWithdrawnById(request.userId()).orElseThrow(
                () -> new CustomException(ExceptionCode.USER_NOT_FOUND)
        );

        validateRoleIsDifferent(request.role(), user.getRole());
        prohibitSuperRoleAssignment(request.role());
        validateAdminCannotModifySuper(adminRole, user.getRole());

        user.updateRole(request.role());
    }

    @Transactional
    public void suspendUser(SuspendUserReq request, UserRole adminRole) {
        UserStatus userStatus = userStatusRepository.findByUserId(request.userId()).orElseThrow(
                () -> new CustomException(ExceptionCode.USER_NOT_FOUND)
        );

        validateNotAlreadySuspended(userStatus);
        validateAdminCannotModifySuper(adminRole, userStatus.getUser().getRole());

        userStatus.updateForSuspend(LocalDateTime.now(), request.suspensionDays(), request.suspensionReason());
    }

    @Transactional
    public void unSuspendUser(Long userId, UserRole adminRole) {
        UserStatus userStatus = userStatusRepository.findByUserId(userId).orElseThrow(
                () -> new CustomException(ExceptionCode.USER_NOT_FOUND)
        );

        validateNotAlreadyUnsuspended(userStatus);
        validateAdminCannotModifySuper(adminRole, userStatus.getUser().getRole());

        userStatus.updateForUnSuspend();
    }

    public FindApplyListRes findApplyList(ApplyStatus status, Pageable pageable) {
        Page<Apply> applyPage = applyRepository.findAllByStatusWithAnimal(status, pageable);

        // Apply의 animalId를 리스트로 만듦 => shleter와 fetch 조인해서 Animal 객체를 조회 => <animalId, Anmal 객체> 맵 생성
        List<Long> animalIds = applyPage.getContent().stream()
                .map(apply -> apply.getAnimal().getId())
                .toList();

        List<Animal> animals = animalRepository.findByIdsWithShelter(animalIds);
        Map<Long, Animal> animalMap = animals.stream()
                .collect(Collectors.toMap(Animal::getId, animal -> animal));

        List<FindApplyListRes.ApplyDTO> applyDTOS = applyPage.getContent().stream()
                .map(apply -> {
                    Animal animal = animalMap.get(apply.getAnimal().getId());
                    return new FindApplyListRes.ApplyDTO(
                            apply.getId(),
                            apply.getCreatedDate(),
                            animal.getId(),
                            animal.getKind(),
                            animal.getGender(),
                            animal.getAge(),
                            apply.getName(),
                            apply.getTel(),
                            apply.getAddressDetail(),
                            animal.getShelter().getName(),
                            animal.getShelter().getCareTel(),
                            apply.getStatus()
                    );
                }).toList();

        return new FindApplyListRes(applyDTOS, applyPage.getTotalPages());
    }

    @Transactional
    public void changeApplyStatus(ChangeApplyStatusReq request) {
        Apply apply = applyRepository.findByIdWithAnimal(request.id()).orElseThrow(
                () -> new CustomException(ExceptionCode.APPLICATION_NOT_FOUND)
        );

        // 이미 처리 됌
        if (apply.getStatus().equals(ApplyStatus.PROCESSED)) {
            throw new CustomException(ExceptionCode.APPLICATION_ALREADY_PROCESSED);
        }

        // 지원서 상태 변경
        apply.updateApplyStatus(request.status());

        // 입양이 완료된거면 입양 완료 상태로 변경
        if (request.status().equals(ApplyStatus.PROCESSED)) {
            apply.getAnimal().finishAdoption();
        }
    }

    public FindReportListRes findReportList(ReportStatus status, Pageable pageable) {
        Page<Report> reportPage = reportRepository.findAllByStatus(status, pageable);

        List<FindReportListRes.ReportDTO> reportDTOS = reportPage.getContent().stream()
                .map(report -> new FindReportListRes.ReportDTO(
                        report.getId(),
                        report.getCreatedDate(),
                        report.getContentType(),
                        report.getContentId(),
                        report.getType(),
                        report.getReason(),
                        report.getReporter().getNickname(),
                        report.getOffender().getId(),
                        report.getOffender().getNickname(),
                        report.getStatus())
                ).toList();

        return new FindReportListRes(reportDTOS, reportPage.getTotalPages());
    }

    @Transactional
    public void processReport(ProcessReportReq request) {
        Report report = reportRepository.findById(request.id()).orElseThrow(
                () -> new CustomException(ExceptionCode.REPORT_NOT_FOUND)
        );

        // 이미 처리함
        if (report.getStatus().equals(ReportStatus.PROCESSED)) {
            throw new CustomException(ExceptionCode.REPORT_DUPLICATE);
        }

        // 유저 정지 처리
        if (request.hasSuspension()) {
            // SUPER를 정지 시킬 수는 없다 (악용 방지)
            if (report.getOffender().getRole().equals(UserRole.SUPER)) {
                throw new CustomException(ExceptionCode.ADMIN_CANNOT_BE_REPORTED);
            }

            report.getOffender().getStatus()
                    .updateForSuspend(LocalDateTime.now(), request.suspensionDays(), report.getReason());
        }

        // 가림 처리
        if (request.hasBlocking()) {
            processBlocking(report);
        }

        // 신고 내역 완료 처리
        report.updateStatus(ReportStatus.PROCESSED);
    }

    public FindSupportListRes findSupportList(InquiryStatus status, Pageable pageable) {
        Page<Inquiry> inquiryPage = inquiryRepository.findByStatusWithUser(status, pageable);

        List<FindSupportListRes.InquiryDTO> inquiryDTOS = inquiryPage.getContent().stream()
                .map(inquiry -> new FindSupportListRes.InquiryDTO(
                        inquiry.getId(),
                        inquiry.getCreatedDate(),
                        inquiry.getQuestioner().getNickname(),
                        inquiry.getType(),
                        inquiry.getTitle(),
                        inquiry.getStatus())
                )
                .toList();

        return new FindSupportListRes(inquiryDTOS, inquiryPage.getTotalPages());
    }

    public FindSupportByIdRes findSupportById(Long inquiryId) {
        Inquiry inquiry = inquiryRepository.findById(inquiryId).orElseThrow(
                () -> new CustomException(ExceptionCode.INQUIRY_NOT_FOUND)
        );

        return new FindSupportByIdRes(
                inquiry.getId(),
                inquiry.getQuestioner().getNickname(),
                inquiry.getTitle(),
                inquiry.getDescription()
        );
    }

    @Transactional
    public AnswerInquiryRes answerInquiry(AnswerInquiryReq request, Long adminId, Long inquiryId) {
        Inquiry inquiry = inquiryRepository.findById(inquiryId).orElseThrow(
                () -> new CustomException(ExceptionCode.INQUIRY_NOT_FOUND)
        );

        // 답변은 하나만 할 수 있음
        if (inquiry.getAnswer() != null) {
            throw new CustomException(ExceptionCode.INQUIRY_ALREADY_ANSWERED);
        }

        User admin = userRepository.getReferenceById(adminId);
        inquiry.updateAnswer(request.content(), admin);

        // 문의글은 처리 완료
        inquiry.updateStatus(InquiryStatus.PROCESSED);

        return new AnswerInquiryRes(inquiryId);
    }

    public FindFAQListRes findFAQList() {
        List<FAQ> faqs = faqRepository.findAll();

        List<FindFAQListRes.FaqDTO> faqDTOS = faqs.stream()
                .map(faq -> new FindFAQListRes.FaqDTO(faq.getQuestion(), faq.getAnswer(), faq.getType(), faq.isTop()))
                .toList();

        return new FindFAQListRes(faqDTOS);
    }

    @Transactional
    public void createFAQ(CreateFaqReq request) {
        FAQ faq = FAQ.builder()
                .question(request.question())
                .answer(request.answer())
                .type(request.type())
                .isTop(request.isTop())
                .build();

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

        return new FindDashboardStatsRes.AnimalStatsDTO(
                waitingForAdoptionCount,
                adoptionProcessingCount,
                adoptedRecentlyCount,
                adoptedTotalCount
        );
    }

    private List<FindDashboardStatsRes.DailyVisitorDTO> getDailyVisitors(LocalDateTime startDate) {
        List<Visit> visits = visitRepository.findALlWithinDate(startDate);
        Map<LocalDate, Long> dailyVisitors = visits.stream()
                .collect(Collectors.groupingBy(
                        visit -> visit.getDate().toLocalDate(),
                        Collectors.counting()
                ));

        return dailyVisitors.entrySet().stream()
                .map(entry -> new FindDashboardStatsRes.DailyVisitorDTO(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparing(FindDashboardStatsRes.DailyVisitorDTO::date))
                .toList();
    }

    private List<FindDashboardStatsRes.HourlyVisitorDTO> getHourlyVisitors(LocalDateTime startOfDay) {
        List<Visit> visits = visitRepository.findALlWithinDate(startOfDay);
        Map<LocalTime, Long> hourlyVisitors = visits.stream()
                .filter(visit -> visit.getDate().toLocalDate().isEqual(LocalDate.now()))
                .collect(Collectors.groupingBy(
                        visit -> visit.getDate().toLocalTime().truncatedTo(ChronoUnit.HOURS),
                        Collectors.counting()
                ));

        return hourlyVisitors.entrySet().stream()
                .map(entry -> new FindDashboardStatsRes.HourlyVisitorDTO(
                        LocalDateTime.of(LocalDate.now(), entry.getKey()),
                        entry.getValue()
                ))
                .sorted(Comparator.comparing(FindDashboardStatsRes.HourlyVisitorDTO::hour))
                .toList();
    }

    private FindDashboardStatsRes.DailySummaryDTO getDailySummary(LocalDateTime startOfDay) {
        Long entryNum = userRepository.countAllUsersCreatedAfter(startOfDay);
        Long newPostNum = postRepository.countALlWithinDate(startOfDay);
        Long newCommentNum = commentRepository.countALlWithinDate(startOfDay);
        Long newAdoptApplicationNum = applyRepository.countByStatusWithinDate(ApplyStatus.PROCESSING, startOfDay);

        return new FindDashboardStatsRes.DailySummaryDTO(
                entryNum,
                newPostNum,
                newCommentNum,
                newAdoptApplicationNum
        );
    }

    private Map<Long, Visit> getLatestVisits() {
        List<Visit> visits = visitRepository.findAll();
        return visits.stream()
                .collect(Collectors.toMap(
                        visit -> visit.getUser().getId(),
                        visit -> visit,
                        (visit1, visit2) -> visit1.getDate().isAfter(visit2.getDate()) ? visit1 : visit2
                ));
    }

    private Map<Long, Long> getApplyCountByStatus(ApplyStatus status) {
        List<Apply> applies = applyRepository.findByStatus(status);
        return applies.stream()
                .collect(Collectors.groupingBy(
                        apply -> apply.getUser().getId(),
                        Collectors.counting()
                ));
    }

    private FindUserListRes.ApplicantDTO mapToApplicantDTO(
            User user,
            Map<Long, Visit> latestVisitMap,
            Map<Long, Long> processingApplyMap,
            Map<Long, Long> processedApplyMap
    ) {
        return new FindUserListRes.ApplicantDTO(
                user.getId(),
                user.getNickname(),
                user.getCreatedDate(),
                Optional.ofNullable(latestVisitMap.get(user.getId()))
                        .map(Visit::getDate)
                        .orElse(null),
                Optional.ofNullable(processingApplyMap.get(user.getId()))
                        .orElse(0L),
                Optional.ofNullable(processedApplyMap.get(user.getId()))
                        .orElse(0L),
                user.getRole(),
                user.getStatus().isActive(),
                user.getStatus().getSuspensionStart(),
                user.getStatus().getSuspensionDays(),
                user.getStatus().getSuspensionReason()
        );
    }

    private void validateRoleIsDifferent(UserRole requestedRole, UserRole currentRole) {
        if (requestedRole.equals(currentRole)) {
            throw new CustomException(ExceptionCode.DUPLICATE_STATUS);
        }
    }

    private void prohibitSuperRoleAssignment(UserRole requestedRole) {
        if (requestedRole.equals(UserRole.SUPER)) {
            throw new CustomException(ExceptionCode.UNAUTHORIZED_ACCESS);
        }
    }

    private void validateAdminCannotModifySuper(UserRole adminRole, UserRole userRole) {
        // ADMIN 권한의 관리자가 SUPER 권한의 유저를 변경 방지
        if (adminRole.equals(UserRole.ADMIN) && userRole.equals(UserRole.SUPER)) {
            throw new CustomException(ExceptionCode.UNAUTHORIZED_ACCESS);
        }
    }

    private void validateNotAlreadySuspended(UserStatus userStatus) {
        if (userStatus.isNotActive()) {
            throw new CustomException(ExceptionCode.ALREADY_SUSPENDED);
        }
    }

    private void validateNotAlreadyUnsuspended(UserStatus userStatus) {
        if (userStatus.isActive()) {
            throw new CustomException(ExceptionCode.ALREADY_SUSPENDED);
        }
    }

    private void processBlocking(Report report) {
        // 게시글은 가림 처리
        if (report.getContentType() == ContentType.POST) {
            Post post = postRepository.findById(report.getContentId()).orElseThrow(
                    () -> new CustomException(ExceptionCode.BAD_APPROACH)
            );
            post.updateTitle(POST_SCREENED);
            post.processBlock();
        }
        // 댓글은 가림 처리
        else if (report.getContentType() == ContentType.COMMENT) {
            Comment comment = commentRepository.findById(report.getContentId()).orElseThrow(
                    () -> new CustomException(ExceptionCode.BAD_APPROACH)
            );
            comment.updateContent(COMMENT_SCREENED);
        } else {
            throw new CustomException(ExceptionCode.BAD_APPROACH);
        }
    }
}