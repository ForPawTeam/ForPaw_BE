package com.hong.forapw.admin.model.response;

import com.hong.forapw.admin.entity.Visit;
import com.hong.forapw.domain.user.constant.UserRole;
import com.hong.forapw.domain.user.entity.User;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
            String suspensionReason
    ) {

        public static List<ApplicantDTO> fromEntities(List<User> users, Map<Long, Visit> latestVisitMap, Map<Long, Long> processingApplyMap,
                                               Map<Long, Long> processedApplyMap) {
            return users.stream()
                    .map(user -> new FindUserListRes.ApplicantDTO(
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
                    ))
                    .toList();
        }
    }
}