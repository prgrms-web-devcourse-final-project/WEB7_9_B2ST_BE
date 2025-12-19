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
	UNAUTHORIZED_ACCESS(HttpStatus.FORBIDDEN, "A404", "접근 권한이 없습니다."),
	TOKEN_REUSE_DETECTED(HttpStatus.UNAUTHORIZED, "A405", "비정상적인 토큰 사용이 감지되어 로그아웃 되었습니다."),
	
	// 탈퇴 철회
	RECOVERY_TOKEN_NOT_FOUND(HttpStatus.NOT_FOUND, "A406", "복구 토큰을 찾을 수 없습니다."),
	RECOVERY_TOKEN_EXPIRED(HttpStatus.BAD_REQUEST, "A407", "복구 토큰이 만료되었습니다."),
	NOT_WITHDRAWN_MEMBER(HttpStatus.BAD_REQUEST, "A408", "탈퇴 상태가 아닌 회원입니다."),
	WITHDRAWAL_PERIOD_EXPIRED(HttpStatus.BAD_REQUEST, "A409", "복구 가능 기간이 만료되었습니다.");

	private final HttpStatus status;
	private final String code;
	private final String message;
}
