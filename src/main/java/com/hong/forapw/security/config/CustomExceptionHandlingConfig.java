package com.hong.forapw.security.config;

import com.hong.forapw.common.exceptions.CustomException;
import com.hong.forapw.common.exceptions.ExceptionCode;
import com.hong.forapw.common.utils.FilterResponseUtils;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.ExceptionHandlingConfigurer;
import org.springframework.stereotype.Component;

@Component
public class CustomExceptionHandlingConfig {

    public void configure(ExceptionHandlingConfigurer<HttpSecurity> exceptionHandling) {
        exceptionHandling
                .authenticationEntryPoint((request, response, authException) ->
                        FilterResponseUtils.unAuthorized(response, new CustomException(ExceptionCode.LOGIN_REQUIRED)))
                .accessDeniedHandler((request, response, accessDeniedException) ->
                        FilterResponseUtils.forbidden(response, new CustomException(ExceptionCode.UNAUTHORIZED_ACCESS)));
    }
}