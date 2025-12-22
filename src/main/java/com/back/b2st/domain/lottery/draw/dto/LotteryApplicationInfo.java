package com.back.b2st.domain.lottery.draw.dto;

import com.back.b2st.domain.seat.grade.entity.SeatGradeType;

/**
 * 신청 정보 - 신청자, 등급, 인원 수
 */
public record LotteryApplicationInfo(
	Long id,
	SeatGradeType grade,
	Integer quantity
) {
}
