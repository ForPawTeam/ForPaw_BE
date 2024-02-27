package com.hong.ForPaw.controller.DTO;

public class UserRequest {

    public record LoginDTO(String email, String password){}

    public record EmailDTO(String email){}

    public record SendCodeDTO(String email, String validationToken) {}

    public record VerifyCodeDTO(String email, String code){}

    public record JoinDTO(String email, String name, String nickName,
                          String region, String subRegion, String password,
                          String passwordConfirm, String profileURL) {}

}