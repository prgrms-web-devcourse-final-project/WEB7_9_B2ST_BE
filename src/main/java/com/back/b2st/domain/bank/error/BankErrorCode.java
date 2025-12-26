package com.back.b2st.domain.bank.error;

import org.springframework.http.HttpStatus;

import com.back.b2st.global.error.code.ErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum BankErrorCode implements ErrorCode {

	INVALID_BANK_CODE(HttpStatus.BAD_REQUEST, "B400", "유효하지 않은 은행 코드입니다."),
	// 향후, 환불 계좌 등록/수정 api를 분리할 경우에 쓰려고 미리 정의.
	DUPLICATE_REFUND_ACCOUNT(HttpStatus.CONFLICT, "B409", "이미 등록된 환불 계좌가 존재합니다.");

	private final HttpStatus status;
	private final String code;
	private final String message;
}
