package com.back.b2st.domain.seat.seat.error;

import org.springframework.http.HttpStatus;

import com.back.b2st.global.error.code.ErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SeatErrorCode implements ErrorCode {

	VENUE_NOT_FOUND(HttpStatus.NOT_FOUND, "S001", "공연장 정보를 찾을 수 없습니다."),
	SECTION_NOT_FOUND(HttpStatus.NOT_FOUND, "S002", "구역 정보를 찾을 수 없습니다."),
	ALREADY_CREATE_SEAT(HttpStatus.BAD_REQUEST, "S003", "이미 등록된 좌석입니다."),
	SEAT_NOT_FOUND(HttpStatus.NOT_FOUND, "S004", "좌석 정보를 찾을 수 없습니다.");

	private final HttpStatus status;
	private final String code;
	private final String message;
}
