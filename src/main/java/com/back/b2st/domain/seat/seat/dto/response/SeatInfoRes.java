package com.back.b2st.domain.seat.seat.dto.response;

import com.back.b2st.domain.seat.seat.entity.Seat;

public record SeatInfoRes(
	Long seatId,
	String sectionName,
	String rowLabel,
	Integer seatNumber,
	String grade
) {
	public static SeatInfoRes toDetail(Seat seat) {
		return new SeatInfoRes(
			seat.getId(),
			seat.getSectionName(),
			seat.getRowLabel(),
			seat.getSeatNumber(),
			"VIP"   // 임시
		);
	}
}
