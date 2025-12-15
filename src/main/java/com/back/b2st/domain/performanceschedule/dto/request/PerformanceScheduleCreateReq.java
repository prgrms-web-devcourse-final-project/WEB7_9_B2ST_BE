package com.back.b2st.domain.performanceschedule.dto.request;

import java.time.LocalDateTime;

import com.back.b2st.domain.performanceschedule.entity.BookingType;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * 공연 회차 생성 요청 DTO
 */
public record PerformanceScheduleCreateReq(
		//@NotNull
		//Long performanceId,

		@NotNull
		LocalDateTime startAt,

		@NotNull
		@Min(1)
		Integer roundNo,

		@NotNull
		BookingType bookingType,

		@NotNull
		LocalDateTime bookingOpenAt,

		LocalDateTime bookingCloseAt) {
}
