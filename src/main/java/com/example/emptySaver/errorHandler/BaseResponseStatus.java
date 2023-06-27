package com.example.emptySaver.errorHandler;


import lombok.Getter;

@Getter
public enum BaseResponseStatus {

    /**
     * 1000 : 요청 성공
     */
    SUCCESS(true,1000,"요청에 성공하였습니다."),

    /**
     * 2000 : Request 오류
     */

    INVALID_REQUEST(false,2000,"유효하지 않은 요청입니다 (변수를 확인해주세요)"),
    POST_USERS_EXISTS_EMAIL(false, 2021, "이미 가입된 이메일입니다."),
    INVALID_USERID(false, 2220, "존재하지 않는 유저ID입니다."),
    INVALID_EMAIL(false, 2221, "존재하지 않는 이메일입니다."),
    INVALID_PASSWORD(false, 2222, "유효하지 않은 비밀번호입니다."),
    INVALID_CHANGE_ATTEMPT_NAME(false, 2223, "유효하지 않은 변경 시도입니다.(이름이 올바르지 않습니다.)"),
    INVALID_SAVE_ATTEMPT(false,2224,"유효하지 않은 저장 시도입니다.(이미 저장되어 있습니다)"),
    INVALID_DELETE_ATTEMPT(false,2225,"유효하지 않은 삭제 시도입니다."),
    INVALID_FCMTOKEN(false,2226,"FCM 토큰이 유효하지 않습니다"),
    INVALID_FRIEND_ID(false, 2227, "존재하지 않는 친구 ID입니다."),
    INVALID_LABEL_NAME(false, 2228, "존재하지 않는 라벨 이름입니다."),
    INVALID_TEAM_ID(false, 2229, "존재하지 않는 팀 ID입니다."),
    INVALID_TEAM_MODIFY(false, 2230, "그룹장을 탈퇴 시킬 수 없습니다 (그룹장을 변경 후 탈퇴 하거나 그룹 자체를 삭제해주세요)"),
    INVALID_MAKE_TEAM_ATTEMPT(false, 2231, "유효하지 않은 그룹 생성 시도 입니다. (이미 동일한 이름의 그룹이 존재합니다.)"),
    INVALID_CATEGORY_ID(false, 2232, "유효하지 않은 카테고리 입니다."),
    NOT_ALLOWED(false, 2233, "허용되지 않은 요청입니다.(권한을 확인해주세요)"),
    MAX_MEMBER_ERROR(false,2234,"해당 그룹의 최대 정원을 초과하는 요청은 허용되지 않습니다."),
    NOT_BELONG_ERROR(false,2235,"해당 그룹에 속해야 요청할 수 있습니다."),
    ALREADY_BELONG_ERROR(false,2235,"이미 해당 그룹에 속해있습니다."),
    NOT_ALONE_ERROR(false,2236,"해당 그룹에 혼자가 아닙니다."),
    NOT_PUBLIC_ERROR(false,2237,"비공개 그룹 입니다."),
    INVALID_COMMENT_ID(false,2238,"유효하지 않은 댓글 ID입니다."),
    INVALID_POST_ID(false,2239,"유효하지 않은 게시글 ID입니다."),


    NOT_EXIST_SCHEDULE_ID(false, 2500,"존재하지 않는 schedule id 입니다."),
    LOCAL_DATE_TIME_END_ERROR(false, 2501,"종료 시간이 시작 시간보다 빠를 수 없습니다."),
    INVALID_NOTIFICATION_ID(false, 2502,"유효하지 않은 알림 ID 입니다"),
    NOT_AVAILABLE_SCHEDULE_FORM(false, 2503,"유효하지 않은 스케줄 형식을 저장하려 합니다."),
    TIME_IS_OVERLAP(false, 2504,"이미 저장된 스케줄과 겹칩니다."),

    RESPONSE_ERROR(false, 3000, "값을 불러오는데 실패하였습니다."),
    DUPLICATE_FRIEND_REQUEST(false, 3001, "중복된 친구 요청 입니다."),
    FAILED_TO_SEND_NOTIFICATION(false, 3010, "알림 전송에 실패했습니다."),
    FAILED_TO_LOGIN(false, 3014, "가입된 유저가 아닙니다."),
    FAILED_TO_MAKE_TEAM(false, 3015, "가능한 그룹 생성 갯수를 초과했습니다."),
    FAILED_TO_ADD_MEMBER_TO_TEAM(false, 3016, "그룹에 회원을 추가하는데 실패했습니다.(이미 추가된 유저입니다)"),
    FAILED_TO_SEND_MAIL(false,3111,"메일 전송에 실패했습니다.");


    private final boolean isSuccess;

    private final int code;

    private final String message;


    private BaseResponseStatus(boolean isSuccess, int code, String message) {
        this.isSuccess = isSuccess;
        this.code = code;
        this.message = message;
    }
}
