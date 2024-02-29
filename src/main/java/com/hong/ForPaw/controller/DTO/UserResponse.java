package com.hong.ForPaw.controller.DTO;

public class UserResponse {
    public record loginDTO(String accessToken, String refreshToken) {}

    public record kakaoLoginDTO(String accessToken, String refreshToken, String email) {}

    public record AccessTokenDTO(String accessToken) {}

    public record ProfileDTO(String name, String nickName, String region, String subRegion, String profileURL) {}
}
