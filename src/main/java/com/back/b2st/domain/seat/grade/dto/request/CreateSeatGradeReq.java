package com.back.b2st.domain.seat.grade.dto.request;

public record CreateSeatGradeReq(
	Long seatId,
	String grade,
	Integer price
) {
}
