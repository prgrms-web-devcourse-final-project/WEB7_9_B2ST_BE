package com.back.b2st.domain.seat.grade.dto.request;

public record CreateSeatGradeReq(
	Long performanceId,
	Long seatId,
	String grade,
	Integer price
) {
}
