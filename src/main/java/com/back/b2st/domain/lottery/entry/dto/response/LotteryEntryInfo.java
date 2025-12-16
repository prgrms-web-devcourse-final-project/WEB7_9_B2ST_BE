package com.back.b2st.domain.lottery.entry.dto.response;

import com.back.b2st.domain.lottery.entry.entity.LotteryEntry;

public record LotteryEntryInfo(
	Long id,
	Long memberId,
	Long performanceId,
	Long scheduleId,
	Long seatGradeId,
	Integer quantity,
	String status
) {
	public static LotteryEntryInfo from(LotteryEntry entry) {
		return new LotteryEntryInfo(
			entry.getId(),
			entry.getMemberId(),
			entry.getPerformanceId(),
			entry.getScheduleId(),
			entry.getSeatGradeId(),
			entry.getQuantity(),
			entry.getStatus().toString()
		);
	}
}
