package com.back.b2st.domain.performance.dto.response;

import java.time.LocalDateTime;
import java.util.List;

import com.back.b2st.domain.performance.entity.Performance;
import com.back.b2st.domain.performance.entity.PerformanceStatus;
import com.back.b2st.domain.seat.grade.entity.SeatGradeType;
import com.back.b2st.domain.venue.venue.entity.Venue;

public record PerformanceDetailRes(
	Long performanceId,    //공연ID
	String title, //공연제목
	String category,    //장르
	String posterUrl, //포스터URL
	String description,    //공연설명
	LocalDateTime startDate,    //공연시작일
	LocalDateTime endDate,    //공연종료일
	PerformanceStatus status,    //공연상태
	VenueSummary venue,    //공연장 정보
	List<GreadPrice> greadPrices
) {
	public record GreadPrice(
		SeatGradeType gradeType,
		Integer price
	) {
	}

	public static PerformanceDetailRes from(Performance performance) {
		return new PerformanceDetailRes(
			performance.getPerformanceId(),
			performance.getTitle(),
			performance.getCategory(),
			performance.getPosterUrl(),
			performance.getDescription(),
			performance.getStartDate(),
			performance.getEndDate(),
			performance.getStatus(),
			VenueSummary.from(performance.getVenue()),
			// todo 등급, 가격 정보 데이터도 받아오도록
			List.of(
				new GreadPrice(SeatGradeType.VIP, 30000),
				new GreadPrice(SeatGradeType.STANDARD, 10000)
			)
		);
	}

	public record VenueSummary(
		Long venueId,
		String name
	) {
		public static VenueSummary from(Venue venue) {
			return new VenueSummary(
				venue.getVenueId(),
				venue.getName()
			);
		}
	}
}
