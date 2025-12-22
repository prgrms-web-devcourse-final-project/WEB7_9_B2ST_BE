package com.back.b2st.domain.notification.event;

public record NotificationEmailEvent(
	NotificationEventType type,
	Long recipientMemberId,
	Long performanceId
) {
	public static NotificationEmailEvent tradePurchased(Long buyerId, Long performanceId) {
		return new NotificationEmailEvent(NotificationEventType.TRADE_PURCHASED, buyerId, performanceId);
	}

	public static NotificationEmailEvent tradeSold(Long sellerId, Long performanceId) {
		return new NotificationEmailEvent(NotificationEventType.TRADE_SOLD, sellerId, performanceId);
	}

	public static NotificationEmailEvent exchangeRequested(Long ownerId, Long performanceId) {
		return new NotificationEmailEvent(NotificationEventType.EXCHANGE_REQUESTED, ownerId, performanceId);
	}

	public static NotificationEmailEvent exchangeAccepted(Long requesterId, Long performanceId) {
		return new NotificationEmailEvent(NotificationEventType.EXCHANGE_ACCEPTED, requesterId, performanceId);
	}

	public static NotificationEmailEvent exchangeRejected(Long requesterId, Long performanceId) {
		return new NotificationEmailEvent(NotificationEventType.EXCHANGE_REJECTED, requesterId, performanceId);
	}
}

