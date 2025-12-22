package com.back.b2st.domain.seat.grade.dto;

import com.back.b2st.domain.seat.grade.entity.SeatGradeType;

public record GradeSeatCount(
	SeatGradeType garde,
	Long count
) {
}
