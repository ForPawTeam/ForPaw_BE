package com.hong.forapw.security.config;

import com.hong.forapw.auth.oauth.common.OAuthToken;
import com.hong.forapw.auth.oauth.common.SocialOAuthService;
import com.hong.forapw.auth.oauth.SocialProvider;
import com.hong.forapw.auth.oauth.common.SocialUser;
import com.hong.forapw.auth.oauth.google.GoogleOAuthService;
import com.hong.forapw.auth.oauth.kakao.KakaoOAuthService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class SocialConfig {

    @Bean
    public Map<SocialProvider, SocialOAuthService<? extends OAuthToken, ? extends SocialUser>> socialServices(
            KakaoOAuthService kakaoOAuthService,
            GoogleOAuthService googleOAuthService
    ) {
        Map<SocialProvider, SocialOAuthService<? extends OAuthToken, ? extends SocialUser>> map = new HashMap<>();
        map.put(SocialProvider.KAKAO, kakaoOAuthService);
        map.put(SocialProvider.GOOGLE, googleOAuthService);

        return map;
    }
}