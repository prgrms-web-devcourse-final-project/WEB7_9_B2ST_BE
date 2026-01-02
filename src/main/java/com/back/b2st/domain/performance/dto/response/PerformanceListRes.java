package com.back.b2st.domain.performance.dto.response;

import com.back.b2st.domain.performance.entity.Performance;
import java.time.LocalDateTime;

public record PerformanceListRes(
	Long performanceId,
	String title,
	String category,
	String posterUrl,
	String venueName,
	LocalDateTime startDate,
	LocalDateTime endDate,
	LocalDateTime bookingOpenAt,
	LocalDateTime bookingCloseAt,
	boolean isBookable
) {
	public static PerformanceListRes from(Performance performance,LocalDateTime now, String resolvedPosterUrl) {
		return new PerformanceListRes(
			performance.getPerformanceId(),
			performance.getTitle(),
			performance.getCategory(),
			resolvedPosterUrl,
			performance.getVenue().getName(),
			performance.getStartDate(),
			performance.getEndDate(),
			performance.getBookingOpenAt(),
			performance.getBookingCloseAt(),
			performance.isBookable(now)
		);
	}
}
