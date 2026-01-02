package com.back.b2st.domain.performance.dto.request;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 공연 생성 요청 DTO
 */
public record CreatePerformanceReq(
	@NotNull
	Long venueId,

	@NotBlank
	@Size(max = 200)
	String title,

	@NotBlank
	@Size(max = 50)
	String category,

	@Size(max = 500)
	String posterUrl,

	@Size(max = 5000)
	String description,

	@NotNull
	LocalDateTime startDate,

	@NotNull
	LocalDateTime endDate
) {
}

