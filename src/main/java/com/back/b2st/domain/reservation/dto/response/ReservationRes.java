package com.back.b2st.domain.reservation.dto.response;

import java.util.List;

import com.back.b2st.domain.reservation.entity.Reservation;

public record ReservationRes(
	Long reservationId,
	Long memberId,
	Long scheduleId,
	Long seatId,
	String reservationStatus
) {
	public static ReservationRes from(Reservation reservation) {
		return new ReservationRes(
			reservation.getId(),
			reservation.getMemberId(),
			reservation.getScheduleId(),
			reservation.getSeatId(),
			reservation.getStatus().name()
		);
	}

	public static List<ReservationRes> fromList(List<Reservation> reservations) {
		return reservations.stream()
			.map(ReservationRes::from)
			.toList();
	}
}
