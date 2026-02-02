package com.back.b2st.domain.prereservation.policy.dto.response;

import java.time.LocalDateTime;

import com.back.b2st.domain.prereservation.policy.entity.PrereservationTimeTable;

public record PrereservationTimeTableRes(
	Long scheduleId,
	Long sectionId,
	LocalDateTime bookingStartAt,
	LocalDateTime bookingEndAt
) {
	public static PrereservationTimeTableRes from(PrereservationTimeTable timeTable) {
		return new PrereservationTimeTableRes(
			timeTable.getPerformanceScheduleId(),
			timeTable.getSectionId(),
			timeTable.getBookingStartAt(),
			timeTable.getBookingEndAt()
		);
	}
}

