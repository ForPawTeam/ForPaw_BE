package com.hong.forapw.domain.user.model.response;

import com.hong.forapw.domain.user.entity.User;

public record FindCommunityRecordRes(
        String nickName,
        String email,
        Long postNum,
        Long commentNum,
        Long questionNum,
        Long answerNum
) {
    public FindCommunityRecordRes(User user, Long postNum, Long commentNum, Long questionNum, Long answerNum) {
        this(
                user.getNickname(),
                user.getEmail(),
                postNum,
                commentNum,
                questionNum,
                answerNum
        );
    }
}