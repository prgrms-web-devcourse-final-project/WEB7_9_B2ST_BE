package com.back.b2st.domain.reservation.entity;

public enum ReservationStatus {
	PENDING,    // 결제 대기 (무통장)
	PAID,    // 결제 완료
	CANCELLED,    // 유효하지 않은 예매
	EXPIRED    // 결제 기한 초과
}