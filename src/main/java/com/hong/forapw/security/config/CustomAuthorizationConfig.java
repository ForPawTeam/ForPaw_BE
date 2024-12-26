package com.hong.forapw.security.config;

import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.stereotype.Component;

@Component
public class CustomAuthorizationConfig {

    public void configure(AuthorizeHttpRequestsConfigurer<?>.AuthorizationManagerRequestMatcherRegistry auth) {
        auth
                .requestMatchers(SecurityRoutes.AUTHENTICATED_ROUTES).authenticated()
                .requestMatchers(SecurityRoutes.PUBLIC_ROUTES).permitAll()
                .anyRequest().authenticated();
    }
}

