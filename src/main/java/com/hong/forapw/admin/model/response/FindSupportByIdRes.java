package com.hong.forapw.admin.model.response;

public record FindSupportByIdRes(
        Long id,
        String questionerNick,
        String title,
        String description) {
}