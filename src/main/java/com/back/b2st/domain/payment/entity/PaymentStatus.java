package com.back.b2st.domain.payment.entity;

import java.util.Collections;
import java.util.List;

import lombok.Getter;

@Getter
public enum PaymentStatus {
	READY("결제 준비"),
	WAITING_FOR_DEPOSIT("입금 대기"),
	DONE("결제 완료"),
	FAILED("결제 실패"),
	CANCELED("결제 취소"),
	EXPIRED("입금 만료");

	private final String description;
	private List<PaymentStatus> allowedNextStatuses;

	PaymentStatus(String description) {
		this.description = description;
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

	/**
	 * 최종 상태 여부 (전이표 기준: 다음 상태가 없으면 final)
	 */
	public boolean isFinal() {
		return allowedNextStatuses.isEmpty();
	}

	/**
	 * 특정 상태로 전이 가능한지 검증
	 */
	public boolean canTransitionTo(PaymentStatus targetStatus) {
		return allowedNextStatuses.contains(targetStatus);
	}
}
