package com.back.b2st.domain.performance.dto;

import java.time.LocalDateTime;

public record PerformanceListRes(
		Long performanceId, //공연명
		String title, //공연제목
		String category, //장르
		String posterUrl, //포스터URL
		String venueName, //공연장명
		LocalDateTime startDate, //공연시작일
		LocalDateTime endDate //공연종료일
) {
	public static PerformanceListRes of(
			Long performanceId,
			String title,
			String category,
			String posterUrl,
			String venueName,
			LocalDateTime startDate,
			LocalDateTime endDate
	) {
		return new PerformanceListRes(
				performanceId,
				title,
				category,
				posterUrl,
				venueName,
				startDate,
				endDate
		);
	}
}
