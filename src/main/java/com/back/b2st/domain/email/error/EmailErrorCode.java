package com.back.b2st.domain.email.error;

import org.springframework.http.HttpStatus;

import com.back.b2st.global.error.code.ErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum EmailErrorCode implements ErrorCode {
	// 인증 코드 관련
	VERIFICATION_NOT_FOUND(HttpStatus.BAD_REQUEST, "E401", "인증 정보를 찾을 수 없습니다. 인증 코드를 다시 요청해주세요."),
	VERIFICATION_CODE_MISMATCH(HttpStatus.BAD_REQUEST, "E402", "인증 코드가 일치하지 않습니다."),
	VERIFICATION_MAX_ATTEMPT(HttpStatus.TOO_MANY_REQUESTS, "E429", "인증 시도 횟수를 초과했습니다. 새 코드를 요청해주세요."),
	// Rate Limiting
	TOO_MANY_REQUESTS(HttpStatus.TOO_MANY_REQUESTS, "E430", "요청이 너무 많습니다. 잠시 후 다시 시도해주세요."),
	// 이미 인증된 회원
	ALREADY_VERIFIED(HttpStatus.CONFLICT, "E409", "이미 이메일 인증이 완료된 계정입니다.");
	private final HttpStatus status;
	private final String code;
	private final String message;
}
