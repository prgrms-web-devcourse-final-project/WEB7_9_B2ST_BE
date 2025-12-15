package com.back.b2st.domain.reservation.error;

import org.springframework.http.HttpStatus;

import com.back.b2st.global.error.code.ErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ReservationErrorCode implements ErrorCode {

	/* ===== 예매 관련 ===== */
	RESERVATION_NOT_FOUND(HttpStatus.NOT_FOUND, "R007", "예매내역이 없습니다."),
	RESERVATION_FORBIDDEN(HttpStatus.FORBIDDEN, "R008", "해당 예매에 대한 권한이 없습니다."),
	RESERVATION_ALREADY_COMPLETED(HttpStatus.CONFLICT, "R009", "이미 결제가 완료된 예매입니다."),
	RESERVATION_ALREADY_CANCELED(HttpStatus.CONFLICT, "R010", "이미 취소된 예매입니다."),

	/* ===== 좌석 관련 ===== */
	SEAT_NOT_FOUND(HttpStatus.NOT_FOUND, "R001", "좌석을 찾을 수 없습니다."),
	SEAT_ALREADY_HOLD(HttpStatus.CONFLICT, "R002", "이미 다른 사용자가 선택한 좌석입니다."),
	SEAT_ALREADY_SOLD(HttpStatus.CONFLICT, "R003", "이미 판매된 좌석입니다."),

	/* ===== 락 관련 ===== */
	SEAT_LOCK_FAILED(HttpStatus.CONFLICT, "R004", "좌석이 이미 선택 중입니다. 잠시 후 다시 시도해주세요."),
	SEAT_LOCK_NOT_OWNED(HttpStatus.FORBIDDEN, "R005", "현재 사용자가 좌석 락을 보유하고 있지 않습니다."),
	SEAT_LOCK_EXPIRED(HttpStatus.GONE, "R006", "좌석 선택 시간이 만료되었습니다."),

	/* ===== 예시 ===== */
	EXAMPLE(HttpStatus.BAD_REQUEST, "R000", "에러코드");

	private final HttpStatus status;
	private final String code;
	private final String message;
}
