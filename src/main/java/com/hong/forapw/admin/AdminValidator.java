package com.hong.forapw.admin;

import com.hong.forapw.admin.constant.ReportStatus;
import com.hong.forapw.admin.entity.Report;
import com.hong.forapw.common.exceptions.CustomException;
import com.hong.forapw.common.exceptions.ExceptionCode;
import com.hong.forapw.domain.apply.constant.ApplyStatus;
import com.hong.forapw.domain.apply.entity.Apply;
import com.hong.forapw.domain.inquiry.entity.Inquiry;
import com.hong.forapw.domain.user.constant.UserRole;
import com.hong.forapw.domain.user.entity.UserStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AdminValidator {


    public void validateRoleIsDifferent(UserRole requestedRole, UserRole currentRole) {
        if (requestedRole.equals(currentRole)) {
            throw new CustomException(ExceptionCode.DUPLICATE_STATUS);
        }
    }

    public void validateAdminCannotModifySuper(UserRole adminRole, UserRole userRole) {
        if (adminRole.equals(UserRole.ADMIN) && userRole.equals(UserRole.SUPER)) {
            throw new CustomException(ExceptionCode.UNAUTHORIZED_ACCESS);
        }
    }

    public void validateNotAlreadySuspended(UserStatus userStatus) {
        if (userStatus.isNotActive()) {
            throw new CustomException(ExceptionCode.ALREADY_SUSPENDED);
        }
    }

    public void validateNotAlreadyUnsuspended(UserStatus userStatus) {
        if (userStatus.isActive()) {
            throw new CustomException(ExceptionCode.ALREADY_SUSPENDED);
        }
    }

    public void validateNotAlreadyProcessed(Apply apply) {
        if (apply.getStatus().equals(ApplyStatus.PROCESSED)) {
            throw new CustomException(ExceptionCode.APPLICATION_ALREADY_PROCESSED);
        }
    }

    public void validateReportNotProcessed(Report report) {
        if (report.getStatus() == ReportStatus.PROCESSED) {
            throw new CustomException(ExceptionCode.REPORT_DUPLICATE);
        }
    }

    public void prohibitSuperRoleAssignment(UserRole requestedRole) {
        if (requestedRole.equals(UserRole.SUPER)) {
            throw new CustomException(ExceptionCode.UNAUTHORIZED_ACCESS);
        }
    }

    public void validateInquiryNotAnswered(Inquiry inquiry) {
        if (inquiry.getAnswer() != null) {
            throw new CustomException(ExceptionCode.INQUIRY_ALREADY_ANSWERED);
        }
    }
}