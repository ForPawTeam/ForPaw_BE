package com.hong.forapw.domain.apply.model.request;

import com.hong.forapw.domain.animal.entity.Animal;
import com.hong.forapw.domain.apply.constant.ApplyStatus;
import com.hong.forapw.domain.apply.entity.Apply;
import com.hong.forapw.domain.user.entity.User;
import jakarta.validation.constraints.NotBlank;

public record ApplyAdoptionReq(
        @NotBlank(message = "지원자 이름을 입력해주세요.")
        String name,
        @NotBlank(message = "연락처를 입력해주세요.")
        String tel,
        @NotBlank(message = "도로명 주소를 입력해주세요.")
        String roadNameAddress,
        @NotBlank(message = "상세 주소를 입력해주세요.")
        String addressDetail,
        @NotBlank(message = "우편 번호를 입력해주세요.")
        String zipCode
) {

    public Apply fromEntity(User user, Animal animal) {
        return Apply.builder()
                .user(user)
                .animal(animal)
                .status(ApplyStatus.PROCESSING)
                .name(name)
                .tel(tel)
                .roadNameAddress(roadNameAddress)
                .addressDetail(addressDetail)
                .zipCode(zipCode)
                .build();
    }
}