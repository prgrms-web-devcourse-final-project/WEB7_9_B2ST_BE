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

	SEAT_HOLD_FORBIDDEN(HttpStatus.FORBIDDEN, "R012", "본인이 선점한 좌석이 아닙니다."),
	SEAT_HOLD_EXPIRED(HttpStatus.GONE, "R013", "좌석 선점 시간이 만료되었습니다.");

	private final HttpStatus status;
	private final String code;
	private final String message;
}
