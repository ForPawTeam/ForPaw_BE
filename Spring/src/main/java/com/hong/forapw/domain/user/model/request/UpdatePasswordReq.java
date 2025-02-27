package com.hong.forapw.domain.user.model.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdatePasswordReq(
        @NotBlank(message = "새 비밀번호를 입력해주세요.")
        @Size(min = 8, max = 20, message = "비밀번호는 8자에서 20자 이내여야 합니다.")
        @Pattern(regexp = "^(?=.*[a-zA-Z])(?=.*\\d)(?=.*[@#$%^&+=!~`,./?;:'\"\\[\\]{}\\\\()|_-])\\S*$", message = "올바른 비밀번호 형식을 입력해주세요.")
        String newPassword,

        @NotBlank(message = "새 비밀번호 확인을 입력해주세요.")
        String newPasswordConfirm,

        @NotBlank(message = "현재 비밀번호를 입력해주세요.")
        String curPassword) {
}
