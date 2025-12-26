package com.back.b2st.domain.lottery.entry.dto.response;

import java.util.UUID;

import com.back.b2st.domain.lottery.entry.entity.LotteryEntry;

/**
 * 추첨 응모 저장
 * @param id    UUID
 * @param memberId    고객id
 * @param performanceId    신청 공연
 * @param scheduleId    신청 회차
 * @param grade    신청 등급
 * @param quantity    신청 인원
 * @param status    APPLIED(응모)
 */
public record LotteryEntryInfo(
	UUID id,
	Long memberId,
	Long performanceId,
	Long scheduleId,
	String grade,
	Integer quantity,
	String status
) {
	public static LotteryEntryInfo from(LotteryEntry entry) {
		return new LotteryEntryInfo(
			entry.getUuid(),
			entry.getMemberId(),
			entry.getPerformanceId(),
			entry.getScheduleId(),
			entry.getGrade().toString(),
			entry.getQuantity(),
			entry.getStatus().toString()
		);
	}
}
