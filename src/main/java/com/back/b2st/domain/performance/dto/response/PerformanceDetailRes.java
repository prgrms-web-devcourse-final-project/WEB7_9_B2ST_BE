package com.back.b2st.domain.performance.dto.response;

import java.time.LocalDateTime;
import java.util.List;

import com.back.b2st.domain.performance.entity.Performance;
import com.back.b2st.domain.performance.entity.PerformanceStatus;
import com.back.b2st.domain.seat.grade.entity.SeatGradeType;
import com.back.b2st.domain.venue.venue.entity.Venue;

/**
 * 공연 상세 응답 DTO
 */
public record PerformanceDetailRes(
	Long performanceId,
	String title,
	String category,
	String posterUrl,  // 최종 URL
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

	/**
	 * Performance 엔티티를 PerformanceDetailRes로 변환
	 *
	 * @param performance 공연 엔티티
	 * @param now 현재 시각
	 * @param gradePrices 등급별 가격 정보
	 * @param resolvedPosterUrl 변환된 최종 포스터 URL (Mapper에서 제공)
	 */
	public static PerformanceDetailRes from(Performance performance, LocalDateTime now, List<GradePrice> gradePrices, String resolvedPosterUrl) {
		return new PerformanceDetailRes(
			performance.getPerformanceId(),
			performance.getTitle(),
			performance.getCategory(),
			resolvedPosterUrl,
			performance.getDescription(),
			performance.getStartDate(),
			performance.getEndDate(),
			performance.getStatus(),
			performance.getBookingOpenAt(),
			performance.getBookingCloseAt(),
			performance.isBookable(now),
			VenueSummary.from(performance.getVenue()),
			gradePrices == null ? List.of() : List.copyOf(gradePrices)
		);
	}

	public record VenueSummary(Long venueId, String name) {
		public static VenueSummary from(Venue venue) {
			return new VenueSummary(venue.getVenueId(), venue.getName());
		}
	}
}
