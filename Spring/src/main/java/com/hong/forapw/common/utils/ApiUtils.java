package com.hong.forapw.common.utils;

import org.springframework.http.HttpStatus;

public class ApiUtils {

    private ApiUtils() {
    }

    public static <T> ApiResult<T> success(HttpStatus httpStatus, T result) {
        return new ApiResult<>(true, httpStatus.value(), httpStatus.getReasonPhrase(), result);
    }

    public static ApiResult<?> error(String message, HttpStatus httpStatus) {

        return new ApiResult<>(false, httpStatus.value(), message, null);
    }

    public record ApiResult<T>(boolean success, int code, String message, T result) {
    }
}