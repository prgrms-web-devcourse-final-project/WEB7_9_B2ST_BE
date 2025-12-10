package com.back.b2st.domain.ticket.entity;

public enum TicketStatus {
	ISSUED,    // 발권
	USED,    // 사용
	CANCELLED,    // 취소
	EXCHANGED,    // 교환
	TRANSFERRED,    // 양도
	EXPIRED;    // 만료
}
