package com.back.b2st.domain.reservation.dto.request;

import java.time.LocalDateTime;
import java.util.List;

import com.back.b2st.domain.reservation.entity.Reservation;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record ReservationReq(

	@NotNull
	Long scheduleId,

	@NotEmpty
	List<Long> seatIds
) {
	public Reservation toEntity(Long memberId, LocalDateTime expiresAt) {
		return Reservation.builder()
			.scheduleId(scheduleId)
			.memberId(memberId)
			.expiresAt(expiresAt)
			.build();
	}
}
