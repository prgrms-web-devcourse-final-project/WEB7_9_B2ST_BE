package com.back.b2st.domain.auth.error;

import org.springframework.http.HttpStatus;

import com.back.b2st.global.error.code.ErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AuthErrorCode implements ErrorCode {
	LOGIN_FAILED(HttpStatus.UNAUTHORIZED, "A401", "이메일 또는 비밀번호가 정확하지 않습니다."),
	EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "A402", "토큰이 만료되었습니다."),
	INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "A403", "유효하지 않은 토큰입니다."),
	UNAUTHORIZED_ACCESS(HttpStatus.FORBIDDEN, "A404", "접근 권한이 없습니다.");

	private final HttpStatus status;
	private final String code;
	private final String message;
}
