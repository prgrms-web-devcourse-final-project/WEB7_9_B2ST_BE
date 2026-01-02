package com.back.b2st.domain.performance.dto.response;

import java.time.LocalDateTime;
import java.util.List;

import com.back.b2st.domain.performance.entity.Performance;
import com.back.b2st.domain.performance.entity.PerformanceStatus;
import com.back.b2st.domain.seat.grade.entity.SeatGradeType;
import com.back.b2st.domain.venue.venue.entity.Venue;

public record PerformanceDetailRes(
	Long performanceId,
	String title,
	String category,
	String posterUrl,
	String description,
	LocalDateTime startDate,
	LocalDateTime endDate,
	PerformanceStatus status,
	LocalDateTime bookingOpenAt,
	LocalDateTime bookingCloseAt,
	boolean isBookable,
	VenueSummary venue,
	List<GradePrice> gradePrices
) {
	public record GradePrice(
		SeatGradeType gradeType,
		Integer price
	) {}

	public static PerformanceDetailRes from(Performance performance, LocalDateTime now, List<GradePrice> gradePrices) {
		return new PerformanceDetailRes(
			performance.getPerformanceId(),
			performance.getTitle(),
			performance.getCategory(),
			performance.getPosterUrl(),
			performance.getDescription(),
			performance.getStartDate(),
			performance.getEndDate(),
			performance.getStatus(),
			performance.getBookingOpenAt(),
			performance.getBookingCloseAt(),
			performance.isBookable(now),
			VenueSummary.from(performance.getVenue()),
			gradePrices == null ? List.of() : gradePrices
		);
	}

	public record VenueSummary(Long venueId, String name) {
		public static VenueSummary from(Venue venue) {
			return new VenueSummary(venue.getVenueId(), venue.getName());
		}
	}
}
