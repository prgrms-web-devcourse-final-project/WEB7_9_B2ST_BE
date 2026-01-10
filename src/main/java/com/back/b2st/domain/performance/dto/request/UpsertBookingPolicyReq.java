package com.back.b2st.domain.performance.dto.request;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotNull;

/**
 * 예매 정책 설정 요청 DTO
 */
public record UpsertBookingPolicyReq(
	@NotNull
	LocalDateTime bookingOpenAt,

	LocalDateTime bookingCloseAt
) {
}

