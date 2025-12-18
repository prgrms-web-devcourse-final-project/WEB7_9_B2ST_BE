package com.back.b2st.domain.lottery.entry.dto.response;

import java.time.LocalDateTime;

import com.back.b2st.domain.lottery.entry.entity.LotteryStatus;
import com.back.b2st.domain.seat.grade.entity.SeatGradeType;

public record AppliedLotteryInfo(
	Long lotteryEntryId,
	String title,    // 공연 제목
	LocalDateTime startAt,    // 시작일
	Integer roundNo, // 회차번호
	SeatGradeType gradeType,    // 신청 등급
	Integer quantity,        // 인원수
	LotteryStatus status    // 결과(응모/당첨/미당첨
) {
}
