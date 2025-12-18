package com.back.b2st.domain.payment.entity;

import java.util.Collections;
import java.util.List;

import lombok.Getter;

@Getter
public enum PaymentStatus {
	READY("결제 준비", true),
	WAITING_FOR_DEPOSIT("입금 대기", true),
	DONE("결제 완료", false),
	FAILED("결제 실패", false),
	CANCELED("결제 취소", false),
	EXPIRED("입금 만료", false);

	private final String description;
	private final boolean canTransition;
	private List<PaymentStatus> allowedNextStatuses;

	PaymentStatus(String description, boolean canTransition) {
		this.description = description;
		this.canTransition = canTransition;
	}

	// enum 상수가 모두 초기화된 후 상태 전이표 설정
	static {
		READY.allowedNextStatuses = List.of(DONE, FAILED, CANCELED);
		WAITING_FOR_DEPOSIT.allowedNextStatuses = List.of(DONE, EXPIRED);
		DONE.allowedNextStatuses = List.of(CANCELED);
		FAILED.allowedNextStatuses = Collections.emptyList();
		CANCELED.allowedNextStatuses = Collections.emptyList();
		EXPIRED.allowedNextStatuses = Collections.emptyList();
	}

	public boolean isCompleted() {
		return this == DONE;
	}

	public boolean isFinal() {
		return !canTransition;
	}

	/**
	 * 특정 상태로 전이 가능한지 검증
	 */
	public boolean canTransitionTo(PaymentStatus targetStatus) {
		return allowedNextStatuses.contains(targetStatus);
	}
}
