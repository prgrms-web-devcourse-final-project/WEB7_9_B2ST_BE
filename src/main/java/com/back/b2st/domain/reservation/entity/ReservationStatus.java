package com.back.b2st.domain.reservation.entity;

public enum ReservationStatus {
	CREATED, // 예매 생성
	PENDING,    // 무통장 입금 대기
	PAID,    // 결제 승인 완료 (카드 승인, 무통장 입금 확인)
	COMPLETED, // 확정
	CANCELED,    // 취소
	EXPIRED    // 결제 기한 초과
}