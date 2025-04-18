package com.hong.forapw.auth.oauth.kakao;

import com.hong.forapw.auth.oauth.common.SocialUser;

public record KakaoUser(
        Long id,
        String connected_at,
        Properties properties,
        Account kakao_account) implements SocialUser {

    public record Properties(
            String nickname,
            String profile_image,
            String thumbnail_image) {
    }

    public record Account(
            String email,
            Boolean profile_nickname_needs_agreement,
            Boolean profile_image_needs_agreement,
            Profile profile) {

        public record Profile(
                String nickname,
                String thumbnail_image_url,
                String profile_image_url,
                Boolean is_default_image) {
        }
    }

    @Override
    public String getEmail() {
        return this.kakao_account != null ? this.kakao_account.email() : null;
    }
}