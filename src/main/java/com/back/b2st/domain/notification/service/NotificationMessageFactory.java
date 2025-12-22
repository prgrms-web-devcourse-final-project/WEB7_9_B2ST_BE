package com.back.b2st.domain.notification.service;

import com.back.b2st.domain.notification.event.NotificationEventType;

public class NotificationMessageFactory {

	private NotificationMessageFactory() {
	}

	public static String subject(NotificationEventType type, String performanceTitle) {
		String title = safeTitle(performanceTitle);
		return switch (type) {
			case TRADE_PURCHASED -> "[TT] 알림 - [양도] " + title + " 구매 완료";
			case TRADE_SOLD -> "[TT] 알림 - [양도] " + title + " 판매 완료";
			case EXCHANGE_REQUESTED -> "[TT] 알림 - [교환] " + title + " 요청 도착";
			case EXCHANGE_ACCEPTED -> "[TT] 알림 - [교환] " + title + " 요청 수락";
			case EXCHANGE_REJECTED -> "[TT] 알림 - [교환] " + title + " 요청 거절";
		};
	}

	public static String message(NotificationEventType type, String performanceTitle) {
		String title = safeTitle(performanceTitle);
		return switch (type) {
			case TRADE_PURCHASED -> "[" + title + "] 양도 구매가 완료되었습니다. 내 티켓에서 확인해주세요.";
			case TRADE_SOLD -> "[" + title + "] 양도가 완료되었습니다. 판매 내역에서 확인해주세요.";
			case EXCHANGE_REQUESTED -> "[" + title + "] 교환 요청이 도착했습니다. 교환 요청 목록에서 확인해주세요.";
			case EXCHANGE_ACCEPTED -> "[" + title + "] 교환 요청이 수락되어 티켓 교환이 완료되었습니다.";
			case EXCHANGE_REJECTED -> "[" + title + "] 교환 요청이 거절되었습니다.";
		};
	}

	private static String safeTitle(String performanceTitle) {
		if (performanceTitle == null || performanceTitle.isBlank()) {
			return "공연";
		}
		return performanceTitle;
	}
}
