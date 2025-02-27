package com.hong.forapw.domain.user.model;

public record TokenDTO(
        String accessToken,
        String refreshToken
) {
}
