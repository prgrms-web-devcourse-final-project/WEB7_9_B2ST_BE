package com.back.b2st.domain.seat.seat.dto.response;

import com.back.b2st.domain.seat.seat.entity.Seat;

public record SeatInfo(
	String sectionName,
	String rowLabel,
	Integer seatNumber
) {
	public static SeatInfo toDetail(Seat seat) {
		return new SeatInfo(
			seat.getSectionName(),
			seat.getRowLabel(),
			seat.getSeatNumber()
		);
	}
}
