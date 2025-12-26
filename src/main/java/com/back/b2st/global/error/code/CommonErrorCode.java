package com.back.b2st.global.error.code;

import org.springframework.http.HttpStatus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CommonErrorCode implements ErrorCode {

	BAD_REQUEST(HttpStatus.BAD_REQUEST, "C400", "잘못된 요청입니다."),
	UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "C401", "인증이 필요합니다."),
	FORBIDDEN(HttpStatus.FORBIDDEN, "C403", "접근 권한이 없습니다."),
	NOT_FOUND(HttpStatus.NOT_FOUND, "C404", "리소스를 찾을 수 없습니다."),
	METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "C405", "허용되지 않은 HTTP 메서드입니다."),
	INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "C500", "서버 내부 오류입니다.");

	private final HttpStatus status;
	private final String code;
	private final String message;
}
