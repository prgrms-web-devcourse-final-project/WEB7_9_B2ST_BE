package com.back.b2st.domain.queue.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.RequiredArgsConstructor;

/**
 * 대기열 입장 상태
 * API 응답에서는 WAITING 상태를 문자열로 표현
 */
@RequiredArgsConstructor
@Schema(description = "대기열 입장 상태")
public enum QueueEntryStatus {

	@Schema(description = "입장 가능 (예매 가능)")
	ENTERABLE("입장 가능"),

	@Schema(description = "입장권 만료")
	EXPIRED("만료됨"),

	@Schema(description = "예매 완료")
	COMPLETED("예매 완료");

	private final String description;

	/**
	 * 최종 상태인지 확인
	 */
	public boolean isFinalState() {
		return this == COMPLETED;
	}

	/**
	 * 재진입 가능한 상태인지 확인
	 */
	public boolean canReenter() {
		return this == EXPIRED;
	}
}

