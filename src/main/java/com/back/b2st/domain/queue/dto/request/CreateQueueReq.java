package com.back.b2st.domain.queue.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 대기열 생성 요청 DTO
 */
public record CreateQueueReq(
	@NotNull(message = "공연 ID는 필수입니다.")
	Long performanceId,

	@NotBlank(message = "대기열 타입은 필수입니다.")
	String queueType,

	@NotNull(message = "최대 동시 입장 인원은 필수입니다.")
	@Min(value = 1, message = "최대 동시 입장 인원은 1 이상이어야 합니다.")
	Integer maxActiveUsers,

	@NotNull(message = "입장권 유효 시간은 필수입니다.")
	@Min(value = 1, message = "입장권 유효 시간은 1분 이상이어야 합니다.")
	Integer entryTtlMinutes
) {
}

