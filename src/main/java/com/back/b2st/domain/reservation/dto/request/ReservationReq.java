package com.back.b2st.domain.reservation.dto.request;

public record ReservationReq(
	Long performanceId,
	Long seatId
) {
}
