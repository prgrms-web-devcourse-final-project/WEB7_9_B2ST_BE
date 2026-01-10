package com.back.b2st.domain.reservation.dto.response;

import com.back.b2st.domain.reservation.entity.Reservation;

public record LotteryReservationCreatedRes(
	Long reservationId,
	Long scheduleId
) {
	public static LotteryReservationCreatedRes from(Reservation reservation) {
		return new LotteryReservationCreatedRes(reservation.getId(), reservation.getScheduleId());
	}
}