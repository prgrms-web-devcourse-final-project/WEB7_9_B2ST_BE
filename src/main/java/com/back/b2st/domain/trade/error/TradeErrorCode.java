package com.back.b2st.domain.trade.error;

import org.springframework.http.HttpStatus;

import com.back.b2st.global.error.code.ErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TradeErrorCode implements ErrorCode {

	TRADE_NOT_FOUND(HttpStatus.NOT_FOUND, "T401", "거래 게시글을 찾을 수 없습니다."),
	UNAUTHORIZED_TRADE_ACCESS(HttpStatus.FORBIDDEN, "T402", "해당 거래에 접근할 권한이 없습니다."),
	INVALID_TRADE_STATUS(HttpStatus.BAD_REQUEST, "T403", "유효하지 않은 거래 상태입니다."),
	INVALID_TRADE_TYPE(HttpStatus.BAD_REQUEST, "T404", "유효하지 않은 거래 유형입니다."),
	TRADE_REQUEST_NOT_FOUND(HttpStatus.NOT_FOUND, "T405", "교환 신청을 찾을 수 없습니다."),
	INVALID_TRADE_REQUEST_STATUS(HttpStatus.BAD_REQUEST, "T406", "유효하지 않은 교환 신청 상태입니다."),
	ALREADY_COMPLETED_TRADE(HttpStatus.BAD_REQUEST, "T407", "이미 완료된 거래입니다."),
	TICKET_NOT_OWNED(HttpStatus.FORBIDDEN, "T408", "보유하지 않은 티켓입니다.");

	private final HttpStatus status;
	private final String code;
	private final String message;
}
