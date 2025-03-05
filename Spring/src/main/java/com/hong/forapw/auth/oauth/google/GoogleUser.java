package com.hong.forapw.auth.oauth.google;

import com.hong.forapw.auth.oauth.common.SocialUser;

public record GoogleUser(
        String id,
        String email,
        String nickname) implements SocialUser {

    @Override
    public String getEmail() {
        return email;
    }
}
