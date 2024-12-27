package com.hong.forapw.domain.user.model.response;

import com.hong.forapw.domain.region.constant.District;
import com.hong.forapw.domain.region.constant.Province;
import com.hong.forapw.domain.user.entity.User;

public record ProfileRes(
        String email,
        String name,
        String nickName,
        Province province,
        District district,
        String subDistrict,
        String profileURL,
        boolean isSocialJoined,
        boolean isShelterOwns,
        boolean isMarketingAgreed) {

    public ProfileRes(User user) {
        this(
                user.getEmail(),
                user.getName(),
                user.getNickname(),
                user.getProvince(),
                user.getDistrict(),
                user.getSubDistrict(),
                user.getProfileURL(),
                user.isSocialJoined(),
                user.isShelterOwns(),
                user.getIsMarketingAgreed()
        );
    }
}