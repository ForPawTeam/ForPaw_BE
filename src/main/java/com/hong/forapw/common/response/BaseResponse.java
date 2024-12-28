package com.hong.forapw.common.response;

import com.hong.forapw.common.utils.ApiUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public class BaseResponse<T> extends ResponseEntity<ApiUtils.ApiResult<T>> {
    public BaseResponse(ApiUtils.ApiResult<T> body) {
        super(body, HttpStatus.OK);
    }

    public static <T> BaseResponse<T> of(HttpStatus status, T body) {
        return new BaseResponse<>(ApiUtils.success(status, body));
    }
}