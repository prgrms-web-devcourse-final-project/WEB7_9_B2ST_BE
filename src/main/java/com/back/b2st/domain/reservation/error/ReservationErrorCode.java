package com.back.b2st.domain.reservation.error;

import org.springframework.http.HttpStatus;

import com.back.b2st.global.error.code.ErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ReservationErrorCode implements ErrorCode {

	/* ===== 예매 조회/권한 ===== */
	RESERVATION_NOT_FOUND(HttpStatus.NOT_FOUND, "R007", "예매내역이 없습니다."),
	RESERVATION_FORBIDDEN(HttpStatus.FORBIDDEN, "R008", "해당 예매에 대한 권한이 없습니다."),

	/* ===== 예매 상태 ===== */
	RESERVATION_ALREADY_COMPLETED(HttpStatus.CONFLICT, "R009", "이미 결제가 완료된 예매입니다."),
	RESERVATION_ALREADY_CANCELED(HttpStatus.CONFLICT, "R010", "이미 취소된 예매입니다."),
	INVALID_RESERVATION_STATUS(HttpStatus.CONFLICT, "R011", "현재 예매 상태에서는 요청을 수행할 수 없습니다."),

	/* ===== 중복/충돌 ===== */
	RESERVATION_ALREADY_EXISTS(HttpStatus.CONFLICT, "R015", "이미 해당 좌석에 대한 예매가 존재합니다.");

	private final HttpStatus status;
	private final String code;
	private final String message;
}
