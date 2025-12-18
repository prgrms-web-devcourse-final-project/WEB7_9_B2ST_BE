package com.back.b2st.domain.reservation.dto.request;

import com.back.b2st.domain.reservation.entity.Reservation;

import jakarta.validation.constraints.NotNull;

public record ReservationReq(

	@NotNull
	Long scheduleId,

	@NotNull
	Long seatId
) {
	public Reservation toEntity(Long memberId) {
		return Reservation.builder()
			.scheduleId(scheduleId)
			.memberId(memberId)
			.seatId(seatId)
			.build();
	}
}
