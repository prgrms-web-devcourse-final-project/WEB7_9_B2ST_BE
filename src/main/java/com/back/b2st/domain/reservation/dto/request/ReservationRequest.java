package com.back.b2st.domain.reservation.dto.request;

public record ReservationRequest(
	Long performanceId,
	Long seatId
) {
}
