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
	RECOVERY_TOKEN_NOT_FOUND(HttpStatus.NOT_FOUND, "A406", "복구 토큰이 유효하지 않거나 만료되었습니다."),
	// 회원 상태 노출 방지를 위해 모호한 메시지 사용
	NOT_WITHDRAWN_MEMBER(HttpStatus.BAD_REQUEST, "A407", "요청을 처리할 수 없습니다."),
	WITHDRAWAL_PERIOD_EXPIRED(HttpStatus.BAD_REQUEST, "A408", "복구 가능 기간(30일)이 만료되었습니다."),

	// 소셜 로그인
	OAUTH_AUTHENTICATION_FAILED(HttpStatus.UNAUTHORIZED, "A409", "소셜 로그인 인증에 실패했습니다."),
	// 카카오 API 호출 오류
	// 액세스 토큰 만료 (이론상 발생 안 함)
	OAUTH_USER_INFO_FAILED(HttpStatus.BAD_GATEWAY, "A410", "소셜 서비스에서 사용자 정보를 가져올 수 없습니다."),
	// 사용자가 이메일 동의하지 않음
	// 프론트에서 재동의 유도 필요
	OAUTH_EMAIL_NOT_PROVIDED(HttpStatus.BAD_REQUEST, "A411", "이메일 정보 제공에 동의해주세요."),
	// 다른 회원이 이미 해당 카카오 계정 사용 중
	// 일반적으로 발생하지 않음 (이메일 기준 조회라서)
	OAUTH_ALREADY_LINKED(HttpStatus.CONFLICT, "A412", "이미 다른 계정에 연동된 소셜 계정입니다."),

	ACCOUNT_LOCKED(HttpStatus.FORBIDDEN, "A413", "로그인 시도 횟수를 초과하여 계정이 일시적으로 잠겼습니다.");

	private final HttpStatus status;
	private final String code;
	private final String message;
}
