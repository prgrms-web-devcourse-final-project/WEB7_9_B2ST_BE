package com.back.b2st.domain.trade.error;

import org.springframework.http.HttpStatus;

import com.back.b2st.global.error.code.ErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TradeErrorCode implements ErrorCode {

	// 비정상적인 경우에만 에러 코드 사용
	TRADE_NOT_FOUND(HttpStatus.NOT_FOUND, "X001", "거래를 찾을 수 없습니다."),
	TRADE_REQUEST_NOT_FOUND(HttpStatus.NOT_FOUND, "X002", "신청을 찾을 수 없습니다."),
	UNAUTHORIZED_TRADE_ACCESS(HttpStatus.FORBIDDEN, "X003", "접근 권한이 없습니다."),
	UNAUTHORIZED_TRADE_REQUEST_ACCESS(HttpStatus.FORBIDDEN, "X004", "접근 권한이 없습니다."),
	INVALID_REQUEST(HttpStatus.BAD_REQUEST, "X005", "잘못된 요청입니다."),
	TRANSFER_PRICE_EXCEEDS_ORIGINAL(HttpStatus.BAD_REQUEST, "X006", "양도 가격은 정가 이하만 가능합니다.");

	private final HttpStatus status;
	private final String code;
	private final String message;
}
