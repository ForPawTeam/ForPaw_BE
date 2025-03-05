package com.hong.forapw.auth.oauth.google;

import com.hong.forapw.auth.oauth.common.OAuthToken;

public record GoogleOAuthToken(
        String access_token,
        Long expires_in,
        String token_type,
        String scope,
        String refresh_token) implements OAuthToken {

    @Override
    public String getToken() {
        return access_token;
    }
}