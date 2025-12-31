package com.back.b2st.domain.payment.dto.request;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PaymentFailReason {
	USER_CANCELED("사용자 결제 취소"),
	PAYMENT_FAILED("결제 실패"),
	TIMEOUT("결제 시간 초과"),
	UNKNOWN("기타");

	private final String description;
}

