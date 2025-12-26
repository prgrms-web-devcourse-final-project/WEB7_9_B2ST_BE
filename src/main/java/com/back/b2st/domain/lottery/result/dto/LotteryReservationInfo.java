package com.back.b2st.domain.lottery.result.dto;

import com.back.b2st.domain.seat.grade.entity.SeatGradeType;

public record LotteryReservationInfo(
	Long resultId,
	Long memberId,
	Long scheduleId,
	SeatGradeType grade,
	Integer quantity
) {
}