package com.back.b2st.domain.seat.seat.dto.response;

import com.back.b2st.domain.seat.grade.entity.SeatGradeType;

public record SeatInfoRes(
	Long seatId,
	String sectionName,
	String rowLabel,
	Integer seatNumber,
	SeatGradeType grade,
	Integer price
) {
}
