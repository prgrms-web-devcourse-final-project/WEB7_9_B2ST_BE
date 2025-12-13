package com.back.b2st.domain.auth.error;

import org.springframework.http.HttpStatus;

import com.back.b2st.global.error.code.ErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AuthErrorCode implements ErrorCode {

	// Login
	LOGIN_FAILED(HttpStatus.UNAUTHORIZED, "A401", "이메일 또는 비밀번호가 일치하지 않습니다."),

	// Reissue
	INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "A402", "유효하지 않은 리프레시 토큰입니다."),
	INVALID_ACCESS_TOKEN(HttpStatus.UNAUTHORIZED, "A403", "유효하지 않은 엑세스 토큰입니다."),
	LOGGED_OUT_USER(HttpStatus.UNAUTHORIZED, "A404", "이미 로그아웃 된 사용자입니다."),
	TOKEN_MISMATCH(HttpStatus.UNAUTHORIZED, "A405", "토큰의 유저 정보가 일치하지 않습니다."),

	// General Token
	EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "A406", "만료된 토큰입니다."),
	UNSUPPORTED_TOKEN(HttpStatus.UNAUTHORIZED, "A407", "지원하지 않는 토큰 형식입니다."),
	EMPTY_CLAIMS(HttpStatus.UNAUTHORIZED, "A408", "토큰에 권한 정보가 없습니다."),

	// User
	USER_NOT_FOUND(HttpStatus.NOT_FOUND, "A409", "해당하는 회원을 찾을 수 없습니다.");

	private final HttpStatus status;
	private final String code;
	private final String message;
}
