package com.back.b2st.domain.performanceschedule.dto.response;

import java.time.LocalDateTime;

import com.back.b2st.domain.performanceschedule.entity.BookingType;
import com.back.b2st.domain.performanceschedule.entity.PerformanceSchedule;

import lombok.Builder;

@Builder
public record PerformanceScheduleListRes(
		Long performanceScheduleId,
		LocalDateTime startAt,
		Integer roundNo,
		BookingType bookingType,
		LocalDateTime bookingOpenAt,
		LocalDateTime bookingCloseAt
) {
	public static PerformanceScheduleListRes from(PerformanceSchedule schedule) {
		return PerformanceScheduleListRes.builder()
				.performanceScheduleId(schedule.getPerformanceScheduleId())
				.startAt(schedule.getStartAt())
				.roundNo(schedule.getRoundNo())
				.bookingType(schedule.getBookingType())
				.bookingOpenAt(schedule.getBookingOpenAt())
				.bookingCloseAt(schedule.getBookingCloseAt())
				.build();
	}
}
