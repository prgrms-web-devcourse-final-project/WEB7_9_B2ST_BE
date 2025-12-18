package com.back.b2st.domain.lottery.entry.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record RegisterLotteryEntryReq(
	// todo 제거하고 헤더, User인증 토큰
	@NotNull(message = "사용자ID는 필수입니다.")
	Long memberId,

	@NotNull(message = "회차 정보는 필수입니다.")
	Long scheduleId,

	@NotBlank(message = "희망 등급은 필수입니다.")
	String grade,

	@NotNull(message = "인원수는 필수입니다.")
	@Positive(message = "인원수는 1 이상이어야 합니다.")
	Integer quantity
) {
}
