package com.back.b2st.domain.reservation.entity;

public enum ReservationStatus {
	CREATED, // 예매 생성
	COMPLETED, // 예매 확정
	CANCELED,    // 예매 취소
	EXPIRED,    // 입금 기한 만료
	PENDING; // TODO: 결제에서 수행해야 할 것 같습니다

	/* === 상태 전이 규칙 === */
	public boolean canComplete() {
		return this == CREATED;
	}

	public boolean canCancel() {
		return this == CREATED;
	}

	public boolean canExpire() {
		return this == CREATED;
	}
}
