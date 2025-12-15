package com.back.b2st.domain.seat.seat.dto.response;

import com.back.b2st.domain.seat.seat.entity.Seat;

public record DetailSeatInfo(
	String sectionName,
	String rowLabel,
	Integer seatNumber
) {
	public DetailSeatInfo(Seat seat) {
		this(
			seat.getSectionName(),
			seat.getRowLabel(),
			seat.getSeatNumber()
		);
	}
}
