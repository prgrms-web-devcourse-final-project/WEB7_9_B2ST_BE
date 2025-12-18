package com.back.b2st.domain.scheduleseat.entity;

public enum SeatStatus {
	AVAILABLE,   // 예매 가능
	HOLD,        // 임시 선점 (Redis 기준)
	SOLD         // 예매 확정
}
