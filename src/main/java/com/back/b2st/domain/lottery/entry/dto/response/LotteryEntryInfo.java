package com.back.b2st.domain.lottery.entry.dto.response;

import java.util.UUID;

import com.back.b2st.domain.lottery.entry.entity.LotteryEntry;

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
