package com.back.b2st.domain.member.error;

import org.springframework.http.HttpStatus;

import com.back.b2st.global.error.code.ErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MemberErrorCode implements ErrorCode {

	// 회원가입 - Account Enumeration 방지를 위해 모호한 메시지 사용
	DUPLICATE_EMAIL(HttpStatus.BAD_REQUEST, "M401", "요청을 처리할 수 없습니다."),
	// 가입 Rate Limiting
	SIGNUP_RATE_LIMIT_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "M408", "가입 요청이 너무 많습니다. 잠시 후 다시 시도해주세요."),
	// 조회
	MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "M403", "해당하는 회원을 찾을 수 없습니다."),
	// 비밀번호 변경
	PASSWORD_MISMATCH(HttpStatus.BAD_REQUEST, "M404", "현재 비밀번호가 일치하지 않습니다."),
	SAME_PASSWORD(HttpStatus.BAD_REQUEST, "M405", "새 비밀번호는 기존 비밀번호와 다르게 설정해야 합니다."),
	// 환불 계좌
	REFUND_ACCOUNT_NOT_FOUND(HttpStatus.NOT_FOUND, "M406", "등록된 환불 계좌가 없습니다."),
	// 회원 탈퇴
	ALREADY_WITHDRAWN(HttpStatus.BAD_REQUEST, "M407", "이미 탈퇴한 회원입니다."),
	PASSWORD_REQUIRED(HttpStatus.BAD_REQUEST, "M409", "비밀번호를 입력해주세요.");

	private final HttpStatus status;
	private final String code;
	private final String message;
}
