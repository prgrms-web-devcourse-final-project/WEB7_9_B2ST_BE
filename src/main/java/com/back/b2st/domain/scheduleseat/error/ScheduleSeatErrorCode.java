package com.back.b2st.domain.scheduleseat.error;

import org.springframework.http.HttpStatus;

import com.back.b2st.global.error.code.ErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ScheduleSeatErrorCode implements ErrorCode {

	/* ===== 좌석 관련 ===== */
	SEAT_NOT_FOUND(HttpStatus.NOT_FOUND, "R001", "좌석을 찾을 수 없습니다."),
	SEAT_ALREADY_HOLD(HttpStatus.CONFLICT, "R002", "이미 다른 사용자가 선택한 좌석입니다."),
	SEAT_ALREADY_SOLD(HttpStatus.CONFLICT, "R003", "이미 판매된 좌석입니다."),

	/* ===== 락 관련 ===== */
	SEAT_LOCK_FAILED(HttpStatus.CONFLICT, "R004", "좌석이 이미 선택 중입니다. 잠시 후 다시 시도해주세요."),
	SEAT_LOCK_NOT_OWNED(HttpStatus.FORBIDDEN, "R005", "현재 사용자가 좌석 락을 보유하고 있지 않습니다."),
	SEAT_LOCK_EXPIRED(HttpStatus.GONE, "R006", "좌석 선택 시간이 만료되었습니다.");

	private final HttpStatus status;
	private final String code;
	private final String message;
}
