package com.hong.forapw.domain.user;

import com.hong.forapw.domain.user.model.UserResponse;
import com.hong.forapw.domain.user.entity.User;
import com.hong.forapw.domain.user.constant.UserRole;

public class UserMapper {

    private UserMapper() {
    }
    
    public static UserResponse.ProfileDTO toProfileDTO(User user) {
        return new UserResponse.ProfileDTO(
                user.getEmail(),
                user.getName(),
                user.getNickname(),
                user.getProvince(),
                user.getDistrict(),
                user.getSubDistrict(),
                user.getProfileURL(),
                user.isSocialJoined(),
                user.isShelterOwns(),
                user.getIsMarketingAgreed());
    }
}
