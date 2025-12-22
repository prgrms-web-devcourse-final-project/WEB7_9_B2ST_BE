package com.back.b2st.domain.notification.event;

public enum NotificationEventType {
	TRADE_PURCHASED, // 내가 양도 구매 완료
	TRADE_SOLD, // 내 양도글 판매 완료
	EXCHANGE_REQUESTED, // 내 교환글에 요청 도착
	EXCHANGE_ACCEPTED, // 내가 요청한 교환 수락
	EXCHANGE_REJECTED // 내가 요청한 교환 거절
}

