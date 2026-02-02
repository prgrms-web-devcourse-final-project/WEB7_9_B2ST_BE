package com.back.b2st.domain.reservation.dto.response;

public record ReservationSeatInfo(
	Long seatId,
	Long sectionId,
	String sectionName,
	String rowLabel,
	Integer seatNumber
) {
}
