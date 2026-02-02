package com.back.b2st.domain.lottery.entry.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

import com.back.b2st.domain.lottery.entry.entity.LotteryStatus;
import com.back.b2st.domain.seat.grade.entity.SeatGradeType;

/**
 * 신청 응모 내역
 * @param lotteryEntryId    응모 uuid
 * @param title    공연 제목
 * @param startAt    공연일
 * @param roundNo    회차번호
 * @param gradeType    신청 등급
 * @param quantity    신청 인원
 * @param status    APPLIED, WIN, LOSE
 */
public record AppliedLotteryInfo(
	UUID lotteryEntryId,
	String title,
	LocalDateTime startAt,
	Integer roundNo,
	SeatGradeType gradeType,
	Integer price,
	Integer quantity,
	LotteryStatus status
) {
}
