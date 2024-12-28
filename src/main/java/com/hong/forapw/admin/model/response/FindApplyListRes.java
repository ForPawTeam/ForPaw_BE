package com.hong.forapw.admin.model.response;

import com.hong.forapw.domain.apply.constant.ApplyStatus;

import java.time.LocalDateTime;
import java.util.List;

public record FindApplyListRes(List<ApplyDTO> applies, int totalPages) {

    public record ApplyDTO(
            Long applyId,
            LocalDateTime applyDate,
            Long animalId,
            String kind,
            String gender,
            String age,
            String userName,
            String tel,
            String residence,
            String careName,
            String careTel,
            ApplyStatus status) {
    }
}