package com.back.b2st.domain.payment.error;

import org.springframework.http.HttpStatus;

import com.back.b2st.global.error.code.ErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PaymentErrorCode implements ErrorCode {

	// 크리티컬: 금액 변조 방지
	AMOUNT_MISMATCH(HttpStatus.BAD_REQUEST, "P001", "결제 금액이 일치하지 않습니다."),

	// 크리티컬: 중복 결제 방지
	DUPLICATE_PAYMENT(HttpStatus.CONFLICT, "P002", "이미 결제가 완료되었습니다."),

	// 크리티컬: 상태 전이 오류
	INVALID_STATUS(HttpStatus.BAD_REQUEST, "P003", "잘못된 결제 상태입니다."),

	// 일반 에러
	NOT_FOUND(HttpStatus.NOT_FOUND, "P004", "결제 정보를 찾을 수 없습니다.");

	private final HttpStatus status;
	private final String code;
	private final String message;
}
