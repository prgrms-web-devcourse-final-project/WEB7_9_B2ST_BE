package com.back.b2st.domain.performanceschedule.dto.response;

import java.time.LocalDateTime;

import com.back.b2st.domain.performanceschedule.entity.BookingType;
import com.back.b2st.domain.performanceschedule.entity.PerformanceSchedule;

public record PerformanceScheduleDetailRes(
		Long performanceScheduleId,
		BookingType bookingType,
		LocalDateTime bookingOpenAt,
		LocalDateTime bookingCloseAt
) {
	public static PerformanceScheduleDetailRes from(PerformanceSchedule schedule) {
		return new PerformanceScheduleDetailRes(
				schedule.getPerformanceScheduleId(),
				schedule.getBookingType(),
				schedule.getBookingOpenAt(),
				schedule.getBookingCloseAt()
		);
	}
}
