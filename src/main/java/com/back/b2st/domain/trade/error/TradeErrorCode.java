package com.back.b2st.domain.trade.error;

import org.springframework.http.HttpStatus;

import com.back.b2st.global.error.code.ErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TradeErrorCode implements ErrorCode {

	TRADE_NOT_FOUND(HttpStatus.NOT_FOUND, "X001", "거래 게시글을 찾을 수 없습니다."),
	UNAUTHORIZED_TRADE_ACCESS(HttpStatus.FORBIDDEN, "X002", "해당 거래에 접근할 권한이 없습니다."),
	INVALID_TRADE_STATUS(HttpStatus.BAD_REQUEST, "X003", "유효하지 않은 거래 상태입니다."),
	INVALID_TRADE_TYPE(HttpStatus.BAD_REQUEST, "X004", "유효하지 않은 거래 유형입니다."),
	TRADE_REQUEST_NOT_FOUND(HttpStatus.NOT_FOUND, "X005", "교환 신청을 찾을 수 없습니다."),
	INVALID_TRADE_REQUEST_STATUS(HttpStatus.BAD_REQUEST, "X006", "유효하지 않은 교환 신청 상태입니다."),
	ALREADY_COMPLETED_TRADE(HttpStatus.BAD_REQUEST, "X007", "이미 완료된 거래입니다."),
	TICKET_NOT_OWNED(HttpStatus.FORBIDDEN, "X008", "보유하지 않은 티켓입니다."),
	TICKET_ALREADY_REGISTERED(HttpStatus.BAD_REQUEST, "X009", "이미 등록된 티켓입니다."),
	INVALID_EXCHANGE_COUNT(HttpStatus.BAD_REQUEST, "X010", "교환은 1개만 가능합니다."),
	INVALID_EXCHANGE_PRICE(HttpStatus.BAD_REQUEST, "X011", "교환은 가격을 설정할 수 없습니다."),
	INVALID_TRANSFER_PRICE(HttpStatus.BAD_REQUEST, "X012", "양도 가격은 필수입니다."),
	CANNOT_UPDATE_EXCHANGE_TRADE(HttpStatus.BAD_REQUEST, "X013", "교환 게시글은 수정할 수 없습니다."),
	CANNOT_DELETE_WITH_PENDING_REQUESTS(HttpStatus.BAD_REQUEST, "X014", "대기 중인 교환 신청이 있어 삭제할 수 없습니다."),
	CANNOT_REQUEST_OWN_TRADE(HttpStatus.BAD_REQUEST, "X015", "자신의 게시글에는 신청할 수 없습니다."),
	DUPLICATE_TRADE_REQUEST(HttpStatus.BAD_REQUEST, "X016", "이미 신청한 게시글입니다."),
	UNAUTHORIZED_TRADE_REQUEST_ACCESS(HttpStatus.FORBIDDEN, "X017", "해당 신청에 접근할 권한이 없습니다."),
	TRADE_ALREADY_HAS_ACCEPTED_REQUEST(HttpStatus.BAD_REQUEST, "X018", "이미 수락된 신청이 있습니다."),
	INVALID_TICKET_COUNT(HttpStatus.BAD_REQUEST, "X019", "티켓은 최소 1개 이상이어야 합니다.");

	private final HttpStatus status;
	private final String code;
	private final String message;
}
