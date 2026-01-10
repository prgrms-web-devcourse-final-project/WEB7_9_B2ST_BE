package com.back.b2st.domain.lottery.result.dto;

import com.back.b2st.domain.seat.grade.entity.SeatGradeType;

/**
 * 응모 정보 조회
 */
public record LotteryReservationInfo(
	Long reservationId,
	Long resultId,
	Long memberId,
	Long scheduleId,
	SeatGradeType grade,
	Integer quantity
) {
}