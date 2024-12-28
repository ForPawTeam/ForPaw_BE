package com.hong.forapw.common.exceptions;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@AllArgsConstructor
@Getter
public enum ExceptionCode {

    // 인증 및 권한 관련 에러
    LOGIN_REQUIRED(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다."),
    INVALID_ACCESS_TOKEN(HttpStatus.BAD_REQUEST, "유효하지 않은 액세스 토큰입니다."),
    TOKEN_EXPIRED(HttpStatus.BAD_REQUEST, "토큰이 만료되었습니다."),
    TOKEN_SIGNATURE_INVALID(HttpStatus.BAD_REQUEST, "토큰 서명이 유효하지 않습니다."),
    UNAUTHORIZED_ACCESS(HttpStatus.FORBIDDEN, "권한이 없습니다."),
    UNAUTHORIZED_UPDATE(HttpStatus.FORBIDDEN, "사용자 정보를 수정할 권한이 없습니다."),
    SUPER_ROLE_CHANGE_FORBIDDEN(HttpStatus.BAD_REQUEST, "SUPER 권한으로 변경할 수 없습니다."),
    INVALID_TOKEN_FORMAT(HttpStatus.BAD_REQUEST, "잘못된 토큰 형식입니다."),

    // 사용자 관련 에러
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."),
    EMAIL_NOT_FOUND(HttpStatus.NOT_FOUND, "이메일을 찾을 수 없습니다."),
    EMAIL_ALREADY_SENT(HttpStatus.TOO_MANY_REQUESTS, "이메일이 이미 발송되었습니다. 잠시 후 다시 시도하세요."),
    EMAIL_DUPLICATE(HttpStatus.BAD_REQUEST, "이미 사용 중인 이메일입니다."),
    NICKNAME_DUPLICATE(HttpStatus.BAD_REQUEST, "이미 사용 중인 닉네임입니다."),
    INVALID_CREDENTIALS(HttpStatus.BAD_REQUEST, "이메일 또는 비밀번호를 다시 확인해 주세요."),
    INVALID_PASSWORD(HttpStatus.BAD_REQUEST, "현재 비밀번호가 올바르지 않습니다."),
    PASSWORD_MISMATCH(HttpStatus.BAD_REQUEST, "비밀번호가 일치하지 않습니다."),
    ACCOUNT_DEACTIVATED(HttpStatus.NOT_FOUND, "탈퇴한 계정입니다."),
    ACCOUNT_SUSPENDED(HttpStatus.FORBIDDEN, "계정이 정지 상태입니다."),
    ALREADY_SUSPENDED(HttpStatus.BAD_REQUEST, "계정이 이미 정지 상태입니다."),
    NOT_SUSPENDED(HttpStatus.BAD_REQUEST, "계정이 정지 상태가 아닙니다."),
    LOGIN_LIMIT_EXCEEDED(HttpStatus.FORBIDDEN, "로그인 시도 횟수를 초과했습니다."),
    ACCOUNT_LOCKED(HttpStatus.FORBIDDEN, "계정이 잠겼습니다. 고객센터에 문의하세요."),
    ADMIN_NOT_FOUND(HttpStatus.NOT_FOUND, "관리자를 찾을 수 없습니다."),
    LOCAL_SIGNUP_ACCOUNT(HttpStatus.BAD_REQUEST, "일반 회원 가입을 통해 가입된 계정입니다."),
    SOCIAL_SIGNUP_ACCOUNT(HttpStatus.BAD_REQUEST, "소셜 계정으로 가입된 계정입니다."),
    REDIRECT_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "로그인 리다이렉션 중 오류가 발생했습니다."),

    // 고객 문의 관련 에러
    INQUIRY_NOT_FOUND(HttpStatus.NOT_FOUND, "문의 내역이 없습니다."),
    INQUIRY_ALREADY_ANSWERED(HttpStatus.BAD_REQUEST, "이미 답변된 문의입니다."),

    // 신고 관련 에러
    INVALID_REPORT_TARGET(HttpStatus.BAD_REQUEST, "잘못된 신고 대상입니다."),
    REPORT_DUPLICATE(HttpStatus.BAD_REQUEST, "이미 신고된 내용입니다."),
    CANNOT_REPORT_OWN_CONTENT(HttpStatus.BAD_REQUEST, "본인의 콘텐츠는 신고할 수 없습니다."),
    REPORT_NOT_FOUND(HttpStatus.NOT_FOUND, "신고 내역이 존재하지 않습니다."),
    ADMIN_CANNOT_BE_REPORTED(HttpStatus.BAD_REQUEST, "관리자는 신고할 수 없습니다."),

    // 게시글 및 댓글 관련 에러
    INVALID_POST_TYPE(HttpStatus.BAD_REQUEST, "유효하지 않은 게시글 타입입니다."),
    POST_NOT_FOUND(HttpStatus.NOT_FOUND, "게시글을 찾을 수 없습니다."),
    CANNOT_LIKE_OWN_POST(HttpStatus.BAD_REQUEST, "본인의 게시글에 좋아요를 누를 수 없습니다."),
    NOT_QUESTION_TYPE(HttpStatus.BAD_REQUEST, "질문글이 아닙니다."),
    NOT_ANSWER_TYPE(HttpStatus.BAD_REQUEST, "답변글이 아닙니다."),
    COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "댓글을 찾을 수 없습니다."),
    NOT_POSTS_COMMENT(HttpStatus.BAD_REQUEST, "게시글의 댓글이 아닙니다."),
    CANNOT_LIKE_OWN_COMMENT(HttpStatus.BAD_REQUEST, "본인의 댓글에 좋아요를 누를 수 없습니다."),
    POST_LIKE_EXPIRED(HttpStatus.BAD_REQUEST, "오래된 게시글은 공감할 수 없습니다."),
    SCREENED_POST(HttpStatus.BAD_REQUEST, "이 게시글은 커뮤니티 규정을 위반하여 숨겨졌습니다."),
    CANT_REPLY_TO_REPLY(HttpStatus.BAD_REQUEST, "대댓글에는 댓글을 달 수 없습니다."),
    POST_MUST_CONTAIN_IMAGE(HttpStatus.BAD_REQUEST, "게시글에는 이미지를 반드시 포함해야 합니다."),

    // 그룹 및 모임 관련 에러
    GROUP_NOT_FOUND(HttpStatus.NOT_FOUND, "그룹을 찾을 수 없습니다."),
    DUPLICATE_GROUP_NAME(HttpStatus.BAD_REQUEST, "이미 사용 중인 그룹 이름입니다."),
    ALREADY_IN_GROUP(HttpStatus.BAD_REQUEST, "이미 그룹에 가입 중이거나 신청이 완료되었습니다."),
    GROUP_NOT_APPLIED(HttpStatus.BAD_REQUEST, "그룹 가입 신청 내역이 없습니다."),
    NOT_GROUP_MEMBER(HttpStatus.BAD_REQUEST, "그룹 멤버가 아닙니다."),
    NOT_GROUP_ADMIN(HttpStatus.BAD_REQUEST, "그룹 관리자가 아닙니다."),
    GROUP_FULL(HttpStatus.BAD_REQUEST, "그룹 인원이 초과되었습니다."),
    ROLE_CANT_UPDATE(HttpStatus.BAD_REQUEST, "그룹장으로의 변경은 불가능합니다."),
    CREATOR_ROLE_UPDATE_FORBIDDEN(HttpStatus.BAD_REQUEST, "그룹장은 자신의 역할을 변경할 수 없습니다."),
    GROUP_CREATOR_CANNOT_LEAVE(HttpStatus.BAD_REQUEST, "그룹장은 권한을 위임한 후에만 탈퇴할 수 있습니다."),
    MEETING_NOT_FOUND(HttpStatus.NOT_FOUND, "정기 모임을 찾을 수 없습니다."),
    MEETING_ALREADY_JOINED(HttpStatus.BAD_REQUEST, "이미 모임에 참가하셨습니다."),
    DUPLICATE_MEETING_NAME(HttpStatus.BAD_REQUEST, "이미 사용 중인 정기 모임 이름입니다."),
    NOT_MEETING_MEMBER(HttpStatus.BAD_REQUEST, "모임 멤버가 아닙니다."),

    // 동물 및 보호소 관련 에러
    ANIMAL_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 동물을 찾을 수 없습니다."),
    ANIMAL_ALREADY_APPLIED(HttpStatus.BAD_REQUEST, "이미 입양 신청을 하셨습니다."),
    ANIMAL_ALREADY_ADOPTED(HttpStatus.BAD_REQUEST, "이미 입양된 동물입니다."),
    APPLICATION_ALREADY_PROCESSED(HttpStatus.BAD_REQUEST, "지원서가 이미 처리되었습니다."),
    APPLICATION_NOT_FOUND(HttpStatus.BAD_REQUEST, "지원서를 찾을 수 없습니다."),
    SHELTER_NOT_FOUND(HttpStatus.NOT_FOUND, "보호소를 찾을 수 없습니다."),
    INVALID_ANIMAL_SORT_TYPE(HttpStatus.BAD_REQUEST, "유효하지 않은 정렬 타입입니다."),

    // 채팅방 및 메시지 관련 에러
    CHAT_ROOM_NOT_FOUND(HttpStatus.NOT_FOUND, "채팅방을 찾을 수 없습니다."),
    MESSAGE_NOT_FOUND(HttpStatus.NOT_FOUND, "메시지를 찾을 수 없습니다."),
    CAN_ACCESS_PREVIOUS_MESSAGE(HttpStatus.BAD_REQUEST, "이전에 조회한 메시지의 페이지만 조회할 수 있습니다."),

    // 검색
    SEARCH_NOT_FOUND(HttpStatus.NOT_FOUND, "검색 결과가 없습니다."),
    SEARCH_KEYWORD_EMPTY(HttpStatus.BAD_REQUEST, "검색어는 비어 있을 수 없습니다."),

    // 알람 관련 에러
    ALARM_NOT_FOUND(HttpStatus.NOT_FOUND, "알림을 찾을 수 없습니다."),
    ALARM_LIST_EMPTY(HttpStatus.NOT_FOUND, "알람 목록이 존재하지 않습니다."),

    // 잘못된 접근 및 요청 관련 에러
    BAD_APPROACH(HttpStatus.BAD_REQUEST, "잘못된 접근입니다."),
    REQUEST_LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST, "요청 횟수를 초과하였습니다."),
    DUPLICATE_STATUS(HttpStatus.BAD_REQUEST, "현재 상태와 동일합니다."),

    // 시스템 관련 에러
    REGION_FILE_NOT_FOUND(HttpStatus.NOT_FOUND, "지역 코드 파일이 존재하지 않습니다."),
    INVALID_URI_FORMAT(HttpStatus.NOT_FOUND, "잘못된 URI 형식입니다."),
    INTRODUCTION_UPDATE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "소개글 업데이트 요청이 실패했습니다."),
    REDIS_LOCK_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "Redis 잠금 요청에 실패했습니다."),
    REDIS_LOCK_INTERRUPTED(HttpStatus.INTERNAL_SERVER_ERROR, "Redis 잠금 처리 중 인터럽트가 발생했습니다."),
    INVALID_TOKEN(HttpStatus.BAD_REQUEST, "잘못된 토큰입니다.");

    private final HttpStatus httpStatus;
    private final String message;
}
