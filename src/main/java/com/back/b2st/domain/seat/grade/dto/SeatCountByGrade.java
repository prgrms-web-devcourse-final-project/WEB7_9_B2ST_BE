package com.back.b2st.domain.seat.grade.dto;

import com.back.b2st.domain.seat.grade.entity.SeatGradeType;

public record SeatCountByGrade(
	SeatGradeType garde,
	Long count
) {
}
