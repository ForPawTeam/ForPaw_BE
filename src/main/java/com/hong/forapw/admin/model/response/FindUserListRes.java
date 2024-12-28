package com.hong.forapw.admin.model.response;

import com.hong.forapw.domain.user.constant.UserRole;

import java.time.LocalDateTime;
import java.util.List;

public record FindUserListRes(List<ApplicantDTO> users, int totalPages) {

    public record ApplicantDTO(
            Long id,
            String nickName,
            LocalDateTime signUpDate,
            LocalDateTime lastLogin,
            Long applicationsSubmitted,
            Long applicationsCompleted,
            UserRole role,
            boolean isActive,
            LocalDateTime suspensionStart,
            Long suspensionDays,
            String suspensionReason) {
    }
}