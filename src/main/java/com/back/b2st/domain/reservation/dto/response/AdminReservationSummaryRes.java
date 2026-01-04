package com.back.b2st.domain.reservation.dto.response;

import java.time.LocalDateTime;

import com.back.b2st.domain.reservation.entity.ReservationStatus;

import lombok.Builder;

@Builder
public record AdminReservationSummaryRes(
	Long reservationId,
	Long scheduleId,
	Long memberId,
	ReservationStatus status,
	Integer seatCount,
	LocalDateTime createdAt,
	LocalDateTime expiresAt
) {
}