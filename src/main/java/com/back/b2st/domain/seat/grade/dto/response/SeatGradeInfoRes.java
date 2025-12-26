package com.back.b2st.domain.seat.grade.dto.response;

import com.back.b2st.domain.seat.grade.entity.SeatGrade;

public record SeatGradeInfoRes(
	Long seatGradeId,
	Long seatId,
	String grade,
	Integer pricd
) {
	public static SeatGradeInfoRes from(SeatGrade seatGrade) {
		return new SeatGradeInfoRes(
			seatGrade.getId(),
			seatGrade.getSeatId(),
			seatGrade.getGrade().toString(),
			seatGrade.getPrice()
		);
	}
}
