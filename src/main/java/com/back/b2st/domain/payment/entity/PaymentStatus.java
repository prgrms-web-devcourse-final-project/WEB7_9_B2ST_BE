package com.back.b2st.domain.payment.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PaymentStatus {
	READY("결제 준비", true),
	WAITING_FOR_DEPOSIT("입금 대기", true),
	DONE("결제 완료", false),
	FAILED("결제 실패", false),
	CANCELED("결제 취소", false),
	EXPIRED("입금 만료", false);

	private final String description;
	private final boolean canTransition;

	public boolean isCompleted() {
		return this == DONE;
	}

	public boolean isFinal() {
		return !canTransition;
	}

	public boolean canCancelFrom() {
		return this == DONE;
	}
}
