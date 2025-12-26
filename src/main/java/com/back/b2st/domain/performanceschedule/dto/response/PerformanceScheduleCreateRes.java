package com.back.b2st.domain.performanceschedule.dto.response;

import java.time.LocalDateTime;

import com.back.b2st.domain.performanceschedule.entity.BookingType;
import com.back.b2st.domain.performanceschedule.entity.PerformanceSchedule;

import lombok.Builder;

@Builder
public record PerformanceScheduleCreateRes(
		Long performanceScheduleId,
		Long performanceId,
		LocalDateTime startAt,
		Integer roundNo,
		BookingType bookingType,
		LocalDateTime bookingOpenAt,
		LocalDateTime bookingCloseAt
) {
	public static PerformanceScheduleCreateRes from(PerformanceSchedule schedule) {
		return PerformanceScheduleCreateRes.builder()
				.performanceScheduleId(schedule.getPerformanceScheduleId())
				.performanceId(schedule.getPerformance().getPerformanceId())
				.startAt(schedule.getStartAt())
				.roundNo(schedule.getRoundNo())
				.bookingType(schedule.getBookingType())
				.bookingOpenAt(schedule.getBookingOpenAt())
				.bookingCloseAt(schedule.getBookingCloseAt())
				.build();
	}
}
