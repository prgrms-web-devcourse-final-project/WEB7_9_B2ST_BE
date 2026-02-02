package com.back.b2st.domain.reservation.dto.response;

import java.time.LocalDateTime;

public record ReservationRes(
	Long reservationId,
	String status,
	PerformanceInfo performance
) {
	public record PerformanceInfo(
		Long performanceId,
		Long performanceScheduleId,
		String title,
		String category,
		LocalDateTime startDate,
		LocalDateTime startAt
	) {
	}
}
