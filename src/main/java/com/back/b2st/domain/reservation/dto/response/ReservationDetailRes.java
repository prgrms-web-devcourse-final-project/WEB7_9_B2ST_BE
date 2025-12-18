package com.back.b2st.domain.reservation.dto.response;

import java.time.LocalDateTime;

public record ReservationDetailRes(
	Long reservationId,
	String status,
	PerformanceInfo performance,
	SeatInfo seat
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

	public record SeatInfo(
		Long seatId,
		Long sectionId,
		String sectionName,
		String rowLabel,
		Integer seatNumber
	) {
	}
}
