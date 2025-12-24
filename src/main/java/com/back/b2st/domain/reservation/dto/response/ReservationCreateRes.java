package com.back.b2st.domain.reservation.dto.response;

import java.time.LocalDateTime;

import com.back.b2st.domain.reservation.entity.Reservation;

public record ReservationCreateRes(
	Long reservationId,
	String status,
	LocalDateTime expiresAt
) {

	public static ReservationCreateRes from(Reservation reservation) {
		return new ReservationCreateRes(
			reservation.getId(),
			reservation.getStatus().name(),
			reservation.getExpiresAt()
		);
	}
}