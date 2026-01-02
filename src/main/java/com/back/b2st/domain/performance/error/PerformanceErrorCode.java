package com.back.b2st.domain.performance.error;

import org.springframework.http.HttpStatus;

import com.back.b2st.global.error.code.ErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 공연 시스템 에러 코드
 */
@Getter
@RequiredArgsConstructor
public enum PerformanceErrorCode implements ErrorCode {

	// 공연 관련
	PERFORMANCE_NOT_FOUND(HttpStatus.NOT_FOUND, "P001", "공연을 찾을 수 없습니다."),
	PERFORMANCE_ALREADY_EXISTS(HttpStatus.CONFLICT, "P002", "이미 존재하는 공연입니다."),
	PERFORMANCE_NOT_ACTIVE(HttpStatus.BAD_REQUEST, "P003", "활성 상태가 아닌 공연입니다."),

	// 공연장 관련
	VENUE_NOT_FOUND(HttpStatus.NOT_FOUND, "P101", "공연장을 찾을 수 없습니다."),

	// 공연 기간 관련
	INVALID_PERFORMANCE_PERIOD(HttpStatus.BAD_REQUEST, "P201", "공연 시작일은 종료일보다 이전이어야 합니다."),
	PERFORMANCE_ALREADY_ENDED(HttpStatus.BAD_REQUEST, "P202", "이미 종료된 공연입니다."),

	// 예매 정책 관련
	INVALID_BOOKING_POLICY(HttpStatus.BAD_REQUEST, "P301", "예매 정책이 유효하지 않습니다."),
	INVALID_BOOKING_TIME(HttpStatus.BAD_REQUEST, "P302", "예매 오픈/마감 시간이 유효하지 않습니다."),
	BOOKING_NOT_OPEN(HttpStatus.BAD_REQUEST, "P303", "예매 오픈 전입니다."),
	BOOKING_CLOSED(HttpStatus.BAD_REQUEST, "P304", "예매가 마감되었습니다."),
	BOOKING_POLICY_NOT_SET(HttpStatus.BAD_REQUEST, "P305", "예매 정책이 설정되지 않았습니다."),

	// 내부 오류
	PERFORMANCE_INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "P999", "공연 시스템 내부 오류가 발생했습니다.");

	private final HttpStatus status;
	private final String code;
	private final String message;
}

