package com.back.b2st.domain.ticket.error;

import org.springframework.http.HttpStatus;

import com.back.b2st.global.error.code.ErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TicketErrorCode implements ErrorCode {

	ALREADY_REGISTERED_TICKET(HttpStatus.BAD_REQUEST, "T001", "이미 등록된 티켓입니다."),
	TICKET_NOT_FOUND(HttpStatus.NOT_FOUND, "T002", "티켓을 찾을 수 없습니다."),
	ALREADY_CANCEL_TICKET(HttpStatus.BAD_REQUEST, "T003", "이미 취소된 티켓입니다."),
	TICKET_NOT_CANCELABLE(HttpStatus.BAD_REQUEST, "T004", "취소할 수 없는 티켓입니다."),
	CREATE_TICKET_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "T005", "티켓 등록 중 오류가 발생했습니다."),
	CANCEL_TICKET_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "T006", "티켓 취소중 오류가 발생했습니다."),
	TICKET_NOT_TRANSFERABLE(HttpStatus.BAD_REQUEST, "T007", "양도할 수 없는 상태의 티켓입니다.");

	private final HttpStatus status;
	private final String code;
	private final String message;
}