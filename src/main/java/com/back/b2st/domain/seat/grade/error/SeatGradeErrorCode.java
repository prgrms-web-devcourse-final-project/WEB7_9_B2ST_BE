package com.back.b2st.domain.seat.grade.error;

import org.springframework.http.HttpStatus;

import com.back.b2st.global.error.code.ErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SeatGradeErrorCode implements ErrorCode {
	PERFORMANCE_NOT_FOUND(HttpStatus.NOT_FOUND, "S001", "공연 정보를 찾을 수 없습니다."),
	SEAT_NOT_FOUND(HttpStatus.NOT_FOUND, "S002", "좌석 정보를 찾을 수 없습니다."),
	GRADE_REQUIRED(HttpStatus.BAD_REQUEST, "S003", "좌석 등급은 필수입니다."),
	INVALID_GRADE_TYPE(HttpStatus.BAD_REQUEST, "S004", "유효하지 않은 좌석 등급입니다."),
	ALREADY_CREATE_SEATGRADE(HttpStatus.CONFLICT, "S005", "이미 등록된 좌석등급입니다.");

	private final HttpStatus status;
	private final String code;
	private final String message;
}
