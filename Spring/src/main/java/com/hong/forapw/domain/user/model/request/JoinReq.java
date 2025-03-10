package com.hong.forapw.domain.user.model.request;

import com.hong.forapw.domain.region.constant.District;
import com.hong.forapw.domain.region.constant.Province;
import com.hong.forapw.domain.user.constant.AuthProvider;
import com.hong.forapw.domain.user.constant.UserRole;
import com.hong.forapw.domain.user.entity.User;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record JoinReq(
        @NotBlank(message = "이메일을 입력해주세요.")
        @Pattern(regexp = "^[\\w._%+-]+@[\\w.-]+\\.[a-zA-Z]{2,6}$", message = "올바른 이메일 형식을 입력해주세요.")
        String email,

        @NotBlank(message = "이름을 입력해주세요.")
        String name,

        @NotBlank(message = "닉네임을 입력해주세요.")
        @Size(min = 2, max = 20, message = "닉네임은 2자에서 20자 이내여야 합니다.")
        String nickName,

        @NotNull(message = "활동 지역을 입력해주세요.")
        Province province,

        @NotNull(message = "활동 지역을 입력해주세요.")
        District district,

        String subDistrict,

        @NotBlank(message = "비밀번호를 입력해주세요.")
        @Size(min = 8, max = 20, message = "비밀번호는 8자에서 20자 이내여야 합니다.")
        @Pattern(regexp = "^(?=.*[a-zA-Z])(?=.*\\d)(?=.*[@#$%^&+=!~`,./?;:'\"\\[\\]{}\\\\()|_-])\\S*$", message = "올바른 비밀번호 형식을 입력해주세요.")
        String password,

        @NotBlank(message = "비밀번호 확인을 입력해주세요.")
        String passwordConfirm,

        String profileURL,
        boolean isShelterOwns,
        boolean isMarketingAgreed
) {
        public User toEntity(String password) {
                return User.builder()
                        .name(name)
                        .nickName(nickName)
                        .email(email)
                        .password(password)
                        .role(isShelterOwns ? UserRole.SHELTER : UserRole.USER)
                        .profileURL(profileURL)
                        .province(province)
                        .district(district)
                        .subDistrict(subDistrict)
                        .authProvider(AuthProvider.LOCAL)
                        .isMarketingAgreed(isMarketingAgreed)
                        .build();
        }
}
