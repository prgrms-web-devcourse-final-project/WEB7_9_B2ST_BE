package com.back.b2st.domain.reservation.dto.request;

import java.time.LocalDateTime;

import com.back.b2st.domain.reservation.entity.Reservation;

import jakarta.validation.constraints.NotNull;

public record ReservationReq(

	@NotNull
	Long scheduleId,

	@NotNull
	Long seatId
) {
	public Reservation toEntity(Long memberId, LocalDateTime expiresAt) {
		return Reservation.builder()
			.scheduleId(scheduleId)
			.memberId(memberId)
			.seatId(seatId)
			.expiresAt(expiresAt)
			.build();
	}
}
