package com.hong.forapw.integration.oauth.google;

import com.hong.forapw.integration.oauth.common.SocialUser;

public record GoogleUser(
        String id,
        String email,
        String nickname) implements SocialUser {

    @Override
    public String getEmail() {
        return email;
    }
}
