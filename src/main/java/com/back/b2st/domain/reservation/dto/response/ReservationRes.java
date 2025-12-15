package com.back.b2st.domain.reservation.dto.response;

public record ReservationRes(
	Long reservationId,
	Long memberId,
	Long performanceId,
	Long seatId
) {
}
