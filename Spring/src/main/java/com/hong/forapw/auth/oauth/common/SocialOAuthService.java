package com.hong.forapw.auth.oauth.common;

public interface SocialOAuthService<T extends OAuthToken, U extends SocialUser> {
    T getToken(String code);
    U getUserInfo(String accessToken);
}