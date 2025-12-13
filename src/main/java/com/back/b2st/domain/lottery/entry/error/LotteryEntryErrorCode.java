package com.back.b2st.domain.lottery.entry.error;

import org.springframework.http.HttpStatus;

import com.back.b2st.global.error.code.ErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum LotteryEntryErrorCode implements ErrorCode {

	INVALID_MEMBER_INFO(HttpStatus.BAD_REQUEST, "L401", "응모자 정보가 올바르지 않습니다."),
	INVALID_PERFORMANCE_INFO(HttpStatus.BAD_REQUEST, "L402", "공연 정보가 올바르지 않습니다."),
	INVALID_SCHEDULE_INFO(HttpStatus.BAD_REQUEST, "L403", "회차 정보가 올바르지 않습니다."),
	PERFORMANCE_SCHEDULE_MISMATCH(HttpStatus.BAD_REQUEST, "L404", "공연 정보와 회차 정보가 일치하지 않습니다."),
	INVALID_GRADE_INFO(HttpStatus.BAD_REQUEST, "L405", "등급 정보가 올바르지 않습니다."),
	EXCEEDS_MAX_ALLOCATION(HttpStatus.BAD_REQUEST, "L406", "응모 가능 수량은 초과할 수 없습니다."),
	DUPLICATE_ENTRY(HttpStatus.CONFLICT, "L407", "이미 존재하는 응모 내역이 있습니다."),

	CREATE_ENTRY_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "L501", "추첨응모 등록 중 서버 오류가 발생했습니다.");

	private final HttpStatus status;
	private final String code;
	private final String message;
}
