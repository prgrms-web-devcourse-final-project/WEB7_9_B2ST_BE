package com.back.b2st.domain.seat.seat.dto.response;

import com.back.b2st.domain.seat.seat.entity.Seat;

public record CreateSeatRes(
	Long seatId,
	Long venueId,
	Long sectionId,
	String sectionName,
	String rowLabel,
	Integer seatNumber
) {
	public static CreateSeatRes from(Seat seat) {
		return new CreateSeatRes(
			seat.getId(),
			seat.getVenueId(),
			seat.getSectionId(),
			seat.getSectionName(),
			seat.getRowLabel(),
			seat.getSeatNumber()
		);
	}
}
