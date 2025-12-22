package com.back.b2st.domain.member.error;

import org.springframework.http.HttpStatus;

import com.back.b2st.global.error.code.ErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MemberErrorCode implements ErrorCode {

	// 회원가입
	DUPLICATE_EMAIL(HttpStatus.CONFLICT, "M401", "이미 가입된 이메일입니다."), // 409 Conflict

	// 조회
	MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "M403", "해당하는 회원을 찾을 수 없습니다."),

	// 비밀번호 변경
	PASSWORD_MISMATCH(HttpStatus.BAD_REQUEST, "M404", "현재 비밀번호가 일치하지 않습니다."),
	SAME_PASSWORD(HttpStatus.BAD_REQUEST, "M405", "새 비밀번호는 기존 비밀번호와 다르게 설정해야 합니다."),

	// 환불 계좌
	REFUND_ACCOUNT_NOT_FOUND(HttpStatus.NOT_FOUND, "M406", "등록된 환불 계좌가 없습니다."),

	// 회원 탈퇴
	ALREADY_WITHDRAWN(HttpStatus.BAD_REQUEST, "M407", "이미 탈퇴한 회원입니다."),
	NOT_WITHDRAWN(HttpStatus.BAD_REQUEST, "M408", "탈퇴 상태가 아닙니다."),
	WITHDRAWAL_PERIOD_EXPIRED(HttpStatus.BAD_REQUEST, "M409", "탈퇴 철회 가능 기간(30일)이 만료되었습니다.");

	private final HttpStatus status;
	private final String code;
	private final String message;
}
