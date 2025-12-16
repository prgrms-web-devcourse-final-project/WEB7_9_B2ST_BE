package com.back.b2st.domain.reservation.dto.request;

import com.back.b2st.domain.reservation.entity.Reservation;

import jakarta.validation.constraints.NotNull;

public record ReservationReq(

	@NotNull
	Long performanceId,

	@NotNull
	Long seatId
) {
	public Reservation toEntity(Long memberId) {
		return Reservation.builder()
			.performanceId(performanceId)
			.memberId(memberId)
			.seatId(seatId)
			.build();
	}
}
