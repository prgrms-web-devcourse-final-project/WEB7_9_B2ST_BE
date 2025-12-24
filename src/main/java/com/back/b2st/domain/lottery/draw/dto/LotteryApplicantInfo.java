package com.back.b2st.domain.lottery.draw.dto;

import com.back.b2st.domain.seat.grade.entity.SeatGradeType;

/**
 * 신청 정보 - 응모id, 등급, 인원 수
 */
public record LotteryApplicantInfo(
	Long id,
	Long memberId,
	SeatGradeType grade,
	Integer quantity
) {
}
