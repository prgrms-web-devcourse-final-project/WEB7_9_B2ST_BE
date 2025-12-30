package com.back.b2st.domain.prereservation.error;

import org.springframework.http.HttpStatus;

import com.back.b2st.global.error.code.ErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PrereservationErrorCode implements ErrorCode {

	SCHEDULE_NOT_FOUND(HttpStatus.NOT_FOUND, "PR001", "공연 회차를 찾을 수 없습니다."),
	BOOKING_TYPE_NOT_SUPPORTED(HttpStatus.BAD_REQUEST, "PR002", "해당 회차는 신청 예매 대상이 아닙니다."),
	APPLICATION_CLOSED(HttpStatus.CONFLICT, "PR003", "신청 예매 기간이 종료되었습니다."),
	SECTION_NOT_FOUND(HttpStatus.NOT_FOUND, "PR004", "구역을 찾을 수 없습니다."),
	SECTION_NOT_IN_VENUE(HttpStatus.BAD_REQUEST, "PR005", "해당 공연장의 구역이 아닙니다."),
	DUPLICATE_APPLICATION(HttpStatus.CONFLICT, "PR006", "이미 신청한 구역입니다."),
	SECTION_NOT_ACTIVATED(HttpStatus.FORBIDDEN, "PR007", "신청한 구역만 예매할 수 있습니다."),
	BOOKING_NOT_OPEN(HttpStatus.FORBIDDEN, "PR008", "예매 오픈 전입니다."),
	BOOKING_CLOSED(HttpStatus.FORBIDDEN, "PR009", "예매가 종료되었습니다.");

	private final HttpStatus status;
	private final String code;
	private final String message;
}
