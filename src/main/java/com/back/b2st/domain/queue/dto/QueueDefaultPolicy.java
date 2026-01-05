package com.back.b2st.domain.queue.dto;

import com.back.b2st.domain.queue.entity.QueueType;

/**
 * 대기열 자동 생성 시 사용하는 기본 정책
 */
public record QueueDefaultPolicy(
	QueueType queueType,
	Integer maxActiveUsers,
	Integer entryTtlMinutes
) {
	/**
	 * 예매용 기본 정책
	 */
	public static QueueDefaultPolicy defaultBooking() {
		return new QueueDefaultPolicy(
			QueueType.BOOKING_ORDER,
			200,
			10
		);
	}
}

