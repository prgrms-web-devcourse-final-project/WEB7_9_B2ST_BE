package com.back.b2st.domain.venue.seat.seat.dto.response;

import com.back.b2st.domain.venue.seat.seat.entity.Seat;

public record CreateSeatRes(
	Long seatId,
	Long venueId,
	Long sectionId,
	String sectionName,
	String rowLabel,
	Integer seatNumber
) {
	public CreateSeatRes(Seat seat) {
		this(
			seat.getId(),
			seat.getVenueId(),
			seat.getSectionId(),
			seat.getSection(),
			seat.getRowLabel(),
			seat.getSeatNumber()
		);
	}
}
