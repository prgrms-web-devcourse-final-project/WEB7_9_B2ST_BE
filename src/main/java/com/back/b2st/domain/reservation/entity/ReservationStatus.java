package com.back.b2st.domain.reservation.entity;

public enum ReservationStatus {

	PENDING,     // 결제 중, 입금 대기
	COMPLETED,     // 예매 확정
	FAILED,         // 카드 결제 실패 등
	CANCELED,     // 예매 취소
	EXPIRED;     // 입금 기한 만료

	/* === 상태 전이 규칙 === */
	public boolean canComplete() {
		return this == PENDING;
	}

	public boolean canCancel() {
		return this == PENDING;
	}

	public boolean canExpire() {
		return this == PENDING;
	}

	public boolean canFail() {
		return this == PENDING;
	}
}