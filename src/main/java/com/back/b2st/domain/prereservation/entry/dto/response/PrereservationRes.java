package com.back.b2st.domain.prereservation.entry.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record PrereservationRes(
	Long scheduleId,
	List<Long> sectionIds,
	LocalDateTime bookingOpenAt,
	LocalDateTime bookingCloseAt
) {
	public static PrereservationRes of(
		Long scheduleId,
		List<Long> sectionIds,
		LocalDateTime bookingOpenAt,
		LocalDateTime bookingCloseAt
	) {
		return new PrereservationRes(scheduleId, sectionIds, bookingOpenAt, bookingCloseAt);
	}

	public static PrereservationRes of(Long scheduleId, List<Long> sectionIds) {
		return new PrereservationRes(scheduleId, sectionIds, null, null);
	}
}
