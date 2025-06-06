package com.minicarrot.user.common;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Constants {

    // 에러 메시지
    public static final class ErrorMessage {
        public static final String EMAIL_ALREADY_EXISTS = "이미 존재하는 이메일입니다.";
        public static final String NICKNAME_ALREADY_EXISTS = "이미 존재하는 닉네임입니다.";
        public static final String USER_NOT_FOUND = "존재하지 않는 사용자입니다.";
        public static final String INVALID_PASSWORD = "비밀번호가 일치하지 않습니다.";
        public static final String INVALID_TOKEN = "유효하지 않은 토큰입니다.";
        public static final String TOKEN_GENERATION_FAILED = "토큰 생성에 실패했습니다.";
        public static final String VALIDATION_FAILED = "입력 데이터가 유효하지 않습니다.";
        public static final String INTERNAL_SERVER_ERROR = "서버 내부 오류가 발생했습니다.";
        
        private ErrorMessage() {}
    }

    // 성공 메시지
    public static final class SuccessMessage {
        public static final String REGISTER_SUCCESS = "회원가입이 완료되었습니다.";
        public static final String LOGIN_SUCCESS = "로그인이 완료되었습니다.";
        public static final String PROFILE_RETRIEVED = "프로필 조회가 완료되었습니다.";
        
        private SuccessMessage() {}
    }

    // JWT 관련
    public static final class JwtConstants {
        public static final String BEARER_PREFIX = "Bearer ";
        public static final String USER_ID_CLAIM = "userId";
        public static final String NICKNAME_CLAIM = "nickname";
        
        private JwtConstants() {}
    }
} 