package com.back.b2st.domain.lottery.result.dto;

import com.back.b2st.domain.seat.grade.entity.SeatGradeType;

/**
 * 결제를 위한 응모 정보
 * @param id    추첨결과 id
 * @param memberId    사용자 id
 * @param seatGrade    신청 등급
 * @param quantity    인원 수
 */

public record LotteryPaymentInfo(
	Long id,
	Long memberId,
	SeatGradeType seatGrade,
	Integer quantity
) {
}
