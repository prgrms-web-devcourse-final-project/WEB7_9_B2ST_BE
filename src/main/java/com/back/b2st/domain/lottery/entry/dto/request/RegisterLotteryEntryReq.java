package com.back.b2st.domain.lottery.entry.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record RegisterLotteryEntryReq(
	@NotNull(message = "사용자ID는 필수입니다.")
	Long memberId,

	@NotNull(message = "회차 정보는 필수입니다.")
	Long scheduleId,

	@NotNull(message = "희망 등급은 필수입니다.")
	Long seatGradeId,

	@NotNull(message = "인원수는 필수입니다.")
	@Min(value = 1, message = "인원수는 1 이상이어야 합니다.")
	Integer quantity
) {
}
