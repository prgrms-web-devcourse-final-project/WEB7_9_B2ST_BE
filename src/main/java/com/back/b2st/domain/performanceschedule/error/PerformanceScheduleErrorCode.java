package com.back.b2st.domain.performanceschedule.error;

import org.springframework.http.HttpStatus;

import com.back.b2st.global.error.code.ErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PerformanceScheduleErrorCode implements ErrorCode {

	PERFORMANCE_NOT_FOUND(HttpStatus.NOT_FOUND, "PS001", "공연을 찾을 수 없습니다."),
	SCHEDULE_NOT_FOUND(HttpStatus.NOT_FOUND, "PS002", "공연 회차를 찾을 수 없습니다."),
	INVALID_BOOKING_TIME(HttpStatus.BAD_REQUEST, "PS003", "예매 오픈/마감 시간이 유효하지 않습니다.");

	private final HttpStatus status;
	private final String code;
	private final String message;
}
