package com.back.b2st.domain.performance.dto.response;

import java.time.LocalDateTime;

import com.back.b2st.domain.performance.entity.Performance;

public record PerformanceListRes(
	Long performanceId,            // 공연 ID
	String title,                  // 공연 제목
	String category,               // 장르
	String posterUrl,              // 포스터 URL
	String venueName,              // 공연장 이름
	LocalDateTime startDate,       // 공연 시작일
	LocalDateTime endDate,         // 공연 종료일
	LocalDateTime bookingOpenAt,   // 예매 오픈 시각
	LocalDateTime bookingCloseAt,  // 예매 마감 시각
	boolean isBookable             // 예매 가능 여부
) {
	public static PerformanceListRes from(Performance performance, LocalDateTime now) {
		return new PerformanceListRes(
			performance.getPerformanceId(),
			performance.getTitle(),
			performance.getCategory(),
			performance.getPosterUrl(),
			performance.getVenue().getName(),
			performance.getStartDate(),
			performance.getEndDate(),
			performance.getBookingOpenAt(),
			performance.getBookingCloseAt(),
			performance.isBookable(now)
		);
	}
}
