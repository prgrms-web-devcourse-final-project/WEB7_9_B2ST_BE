package com.back.b2st.domain.queue.dto.request;

import jakarta.validation.constraints.Min;

/**
 * 대기열 설정 수정 요청 DTO
 */
public record UpdateQueueReq(
	@Min(value = 1, message = "최대 동시 입장 인원은 1 이상이어야 합니다.")
	Integer maxActiveUsers,

	@Min(value = 1, message = "입장권 유효 시간은 1분 이상이어야 합니다.")
	Integer entryTtlMinutes
) {
}