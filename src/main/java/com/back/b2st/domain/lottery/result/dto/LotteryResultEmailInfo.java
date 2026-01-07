package com.back.b2st.domain.lottery.result.dto;

import java.time.LocalDateTime;

import com.back.b2st.domain.seat.grade.entity.SeatGradeType;

public record LotteryResultEmailInfo(
	Long id,
	Long memberId,
	String memberName,           // 추가
	SeatGradeType seatGrade,
	Integer quantity,
	LocalDateTime paymentDeadline       // 추가
) {
}
